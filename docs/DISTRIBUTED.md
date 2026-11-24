# Distributed Systems Testing

This document describes the distributed systems testing capabilities in RaceLab.

## Overview

RaceLab provides distributed systems testing through:

1. **Redis Distributed Locking** - Cross-service coordination
2. **Kafka Event Processing** - Message-driven architectures (Phase E)
3. **Failure Injection** - Network partitions, broker failures (Phase G)

## Redis Distributed Locking

### Implementation

RaceLab includes a Redis-based distributed lock implementation (`RedisDistributedLock`) with:

- **Atomic acquisition** using `SET NX PX` (set if not exists with expiry)
- **Safe release** using Lua script (only delete if we own the lock)
- **Automatic expiry** prevents deadlocks if client crashes
- **Lock extension** for long-running operations

### Usage

```java
// Acquire lock
LockHandle handle = redisDistributedLock.tryLock(
    "booking:concert-2024",  // lock name
    5000,                     // lock timeout (ms)
    10000,                    // acquire timeout (ms)
    cancellationToken         // optional cancellation
);

if (handle != null) {
    try (handle) {
        // Critical section - perform booking
        bookingService.book(eventId, userId, quantity);
    } // Automatic release via try-with-resources
}
```

### Configuration

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: ""  # optional
    database: 0
    timeout: 2000
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### Lock Implementation Details

#### Acquire (SET NX PX)
```redis
SET lock:booking:concert-2024 "uuid-value" NX PX 5000
```
- `NX` - Only set if not exists (atomic check-and-set)
- `PX 5000` - Expire after 5000ms (auto-release if client crashes)

#### Release (Lua Script)
```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```
- Only deletes if we own the lock (prevents releasing another's lock)
- Atomic - no race condition between check and delete

#### Extend (Lua Script)
```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("pexpire", KEYS[1], ARGV[2])
else
    return 0
end
```
- Extends TTL only if we still own the lock

## Booking Service with Redis Lock

`RedisDistributedLockBookingService` implements the `BookingService` interface using distributed locks:

```java
@Service
public class RedisDistributedLockBookingService implements BookingService {
    
    public BookingResult book(String eventId, String userId, int quantity) {
        LockHandle handle = distributedLock.tryLock(
            "booking:" + eventId, 5000, 10000, null);
        
        if (handle == null) {
            return BookingResult.failure("Could not acquire lock", ...);
        }
        
        try (handle) {
            // Critical section with distributed lock
            return performBooking(eventId, userId, quantity);
        }
    }
}
```

## Concurrency Strategies Comparison

| Strategy | Scope | Infrastructure | Latency | Consistency | Failure Mode |
|----------|-------|----------------|---------|-------------|--------------|
| **None** | Local | None | Lowest | ❌ Broken | Data corruption |
| **Optimistic Locking** | Local | DB only | Low | ✅ | Retry storms |
| **Pessimistic Locking** | Local | DB only | Medium | ✅ | Deadlocks |
| **Atomic Update** | Local | DB only | Low | ✅ | Limited logic |
| **Idempotency Key** | Distributed | DB only | Low | ✅* | Client-dependent |
| **Redis Distributed Lock** | Distributed | Redis | Medium | ✅ | Redis SPOF |

*Idempotency keys require client cooperation.

## Benchmarking

### Running Benchmarks

```bash
# Run full benchmark via API
curl -X POST http://localhost:8080/api/benchmarks/run \
  -H "Content-Type: application/json" \
  -d '{
    "name": "strategy-comparison",
    "eventId": "bench-event",
    "totalTickets": 1,
    "concurrentRequests": 100,
    "quantityPerRequest": 1,
    "iterations": 5,
    "strategies": [
      "OPTIMISTIC_LOCKING",
      "PESSIMISTIC_LOCKING",
      "ATOMIC_UPDATE",
      "IDEMPOTENCY_KEY",
      "REDIS_DISTRIBUTED_LOCK"
    ]
  }'

# Quick single-iteration benchmark
curl -X POST "http://localhost:8080/api/benchmarks/run-single?concurrentRequests=100"
```

### Benchmark Configuration

```json
{
  "name": "strategy-comparison",
  "eventId": "bench-event",
  "totalTickets": 1,
  "concurrentRequests": 100,
  "quantityPerRequest": 1,
  "iterations": 5,
  "strategies": [
    "OPTIMISTIC_LOCKING",
    "PESSIMISTIC_LOCKING",
    "ATOMIC_UPDATE",
    "IDEMPOTENCY_KEY",
    "REDIS_DISTRIBUTED_LOCK"
  ]
}
```

### Metrics Collected

Per iteration:
- **Success rate** - % of successful bookings
- **Average latency** - Mean booking latency
- **Percentiles** - p50, p95, p99 latency
- **Throughput** - Requests per second
- **Consistency** - No overselling, no negative inventory

Aggregated across iterations:
- **Avg success rate** - Mean across iterations
- **Avg latency** - Mean across iterations
- **Total successes/failures** - Sum across iterations

## Running Tests

### Local Development

```bash
# Start Redis
docker compose up -d redis

# Run tests
./mvnw test -pl racelab-core -Dtest=RedisDistributedLockBookingServiceConcurrencyTest
./mvnw test -pl racelab-engine -Dtest=BenchmarkRunnerIntegrationTest
```

### CI/CD

Tests use Testcontainers for Redis and PostgreSQL:
- Automatic container lifecycle management
- Isolated test environments
- No external dependencies required

## Known Limitations

### Redis Distributed Lock

1. **Single Redis instance** - Not full Redlock (requires 3+ Redis instances)
2. **Clock drift** - Relies on Redis server clock for TTL
3. **Network partition** - Lock may not be released during partition
3. **SPOF** - Redis is single point of failure

For production, consider:
- **Redisson** - Full Redlock implementation
- **Multiple Redis instances** - Redlock algorithm
- **Redisson** - Handles lock renewal, pub/sub for lock release

### Benchmark Limitations

1. **Single-threaded results collection** - Latencies measured in application
2. **No warm-up iterations** - First iteration may be slower
3. **Local testing only** - Network latency not simulated

## Future Enhancements

1. **Kafka integration** - Event-driven booking with exactly-once semantics
2. **Full Redlock** - Multiple Redis instances for safety
3. **Network simulation** - Latency, partition injection
4. **Distributed tracing** - OpenTelemetry integration
5. **Chaos engineering** - Automated failure injection