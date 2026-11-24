package com.racelab.core.distributed;

import com.racelab.core.util.CancellationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based distributed lock implementation using Redlock algorithm (simplified).
 * 
 * Uses a single Redis instance with Lua script for atomic lock acquisition/release.
 * For production, consider using Redisson or implementing full Redlock with multiple Redis instances.
 * 
 * Key features:
 * - Atomic acquire with SET NX PX (set if not exists with expiry)
 * - Safe release with Lua script (only delete if we own the lock)
 * - Automatic expiry prevents deadlocks if client crashes
 * - Supports cancellation token for cooperative cancellation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDistributedLock {

    private final StringRedisTemplate redisTemplate;

    // Lua script for atomic lock release (only delete if value matches)
    private static final String UNLOCK_SCRIPT = """
        if redis.call("get", KEYS[1]) == ARGV[1] then
            return redis.call("del", KEYS[1])
        else
            return 0
        end
        """;

    // Lua script for atomic lock extension (only extend if value matches)
    private static final String EXTEND_SCRIPT = """
        if redis.call("get", KEYS[1]) == ARGV[1] then
            return redis.call("pexpire", KEYS[1], ARGV[2])
        else
            return 0
        end
        """;

    private final DefaultRedisScript<Long> unlockScript = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
    private final DefaultRedisScript<Long> extendScript = new DefaultRedisScript<>(EXTEND_SCRIPT, Long.class);

    /**
     * Attempt to acquire a distributed lock.
     *
     * @param lockName unique name for the lock
     * @param lockTimeoutMs how long the lock is valid (auto-expires)
     * @param acquireTimeoutMs how long to wait for lock acquisition
     * @param cancellationToken optional cancellation token
     * @return LockHandle if acquired, null if not acquired within timeout
     */
    public LockHandle tryLock(String lockName, long lockTimeoutMs, long acquireTimeoutMs, CancellationToken cancellationToken) {
        String lockKey = "lock:" + lockName;
        String lockValue = UUID.randomUUID().toString();
        long acquireStart = System.currentTimeMillis();
        long acquireEnd = acquireStart + acquireTimeoutMs;

        while (System.currentTimeMillis() < acquireEnd) {
            if (cancellationToken != null && cancellationToken.isCancelled()) {
                log.debug("Lock acquisition cancelled for {}", lockName);
                return null;
            }

            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofMillis(lockTimeoutMs));

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Acquired lock {} with value {}", lockName, lockValue);
                return new LockHandle(lockKey, lockValue, this);
            }

            // Brief pause before retry
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        log.debug("Failed to acquire lock {} within {}ms", lockName, acquireTimeoutMs);
        return null;
    }

    /**
     * Release a previously acquired lock.
     */
    public boolean releaseLock(LockHandle handle) {
        if (handle == null || handle.isReleased()) {
            return false;
        }

        Long result = redisTemplate.execute(unlockScript,
            Collections.singletonList(handle.lockKey()),
            handle.lockValue());

        boolean released = result != null && result == 1;
        if (released) {
            handle.markReleased();
            log.debug("Released lock {}", handle.lockKey());
        }
        return released;
    }

    /**
     * Extend the lock expiry time.
     */
    public boolean extendLock(LockHandle handle, long additionalMs) {
        if (handle == null || handle.isReleased()) {
            return false;
        }

        Long result = redisTemplate.execute(extendScript,
            Collections.singletonList(handle.lockKey()),
            handle.lockValue(),
            String.valueOf(additionalMs));

        return result != null && result == 1;
    }

    /**
     * Check if a lock is currently held.
     */
    public boolean isLocked(String lockName) {
        String lockKey = "lock:" + lockName;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * Get the remaining TTL of a lock in milliseconds.
     */
    public long getLockTtl(String lockName) {
        String lockKey = "lock:" + lockName;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MILLISECONDS);
        return ttl != null ? ttl : -1;
    }

    /**
     * Handle for an acquired lock. Use try-with-resources pattern.
     */
    public static final class LockHandle implements AutoCloseable {
        private final String lockKey;
        private final String lockValue;
        private final RedisDistributedLock lock;
        private volatile boolean released = false;

        public LockHandle(String lockKey, String lockValue, RedisDistributedLock lock) {
            this.lockKey = lockKey;
            this.lockValue = lockValue;
            this.lock = lock;
        }

        public String lockKey() {
            return lockKey;
        }

        public String lockValue() {
            return lockValue;
        }

        public RedisDistributedLock lock() {
            return lock;
        }

        public boolean isReleased() {
            return released;
        }

        public void markReleased() {
            this.released = true;
        }

        @Override
        public void close() {
            if (!released) {
                lock.releaseLock(this);
            }
        }
    }
}