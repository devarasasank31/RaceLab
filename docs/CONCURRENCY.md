# Concurrency Strategies Deep Dive

This document provides a comprehensive analysis of the concurrency control strategies implemented in RaceLab.

## The Problem

When multiple threads/processes access shared data concurrently, race conditions can occur:

1. **Lost Updates** - Thread A reads, Thread B reads, Thread A writes, Thread B writes (B's write loses A's changes)
2. **Duplicate Operations** - Same operation executed multiple times (e.g., double booking)
3. **Inconsistent Reads** - Reading intermediate states during another transaction
4. **Overselling** - Selling more inventory than available

## Strategy Comparison

### 1. No Protection (Baseline)

```java
// Vulnerable code
public void bookTicket(String eventId, String userId) {
    Ticket ticket = ticketRepository.findByEventId(eventId);
    if (ticket.getAvailableQuantity() > 0) {
        ticket.setAvailableQuantity(ticket.getAvailableQuantity() - 1);
        ticket.setBookedBy(userId);
        ticketRepository.save(ticket);
    }
}
```

**Result under 1000 concurrent requests for 1 ticket:**
- Multiple successful bookings (race condition)
- Database state: oversold (negative available quantity possible)
- Data corruption

**Pros:** Maximum throughput, no overhead
**Cons:** Data corruption, incorrect behavior

---

### 2. Optimistic Locking (JPA `@Version`)

```java
@Entity
public class Ticket {
    @Version
    private Long version;
    
    private int availableQuantity;
    
    public void book(String userId) {
        if (availableQuantity <= 0) throw new IllegalStateException("Sold out");
        availableQuantity--;
        bookedBy = userId;
    }
}

// Repository
@Lock(LockModeType.OPTIMISTIC)
@Query("SELECT t FROM Ticket t WHERE t.eventId = :eventId")
Optional<Ticket> findByEventIdForUpdate(@Param("eventId") String eventId);

// Service with retry
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 10))
@Transactional
public void bookTicket(String eventId, String userId) {
    Ticket ticket = ticketRepository.findByEventIdForUpdate(eventId)
        .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
    ticket.book(userId);
    ticketRepository.save(ticket); // OptimisticLockException on conflict
}
```

**Result under 1000 concurrent requests for 1 ticket:**
- Exactly 1 successful booking
- 999 `OptimisticLockException` retries (configurable)
- Database state: consistent

**Pros:**
- No database locks held during transaction
- Good for read-heavy, low-contention workloads
- Works with connection pooling

**Cons:**
- Retry overhead under high contention
- Starvation possible (same thread keeps winning)
- Requires version column
- Application-level retry logic needed

---

### 3. Pessimistic Locking (`SELECT FOR UPDATE`)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM Ticket t WHERE t.eventId = :eventId")
Optional<Ticket> findByEventIdForUpdate(@Param("eventId") String eventId);

@Transactional
public void bookTicket(String eventId, String userId) {
    Ticket ticket = ticketRepository.findByEventIdForUpdate(eventId)
        .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
    ticket.book(userId);
    ticketRepository.save(ticket);
}
```

**Result under 1000 concurrent requests for 1 ticket:**
- Exactly 1 successful booking
- 999 threads block until lock released
- Database state: consistent

**Pros:**
- Strong consistency guarantees
- No application retry logic needed
- Database handles queuing

**Cons:**
- Lock contention reduces throughput
- Deadlock risk with multiple resources
- Holds database connections longer
- Connection pool exhaustion under high load

---

### 4. Atomic Database Update

```java
@Modifying
@Query("""
    UPDATE Ticket t 
    SET t.availableQuantity = t.availableQuantity - :quantity,
        t.bookedQuantity = t.bookedQuantity + :quantity,
        t.status = CASE WHEN t.availableQuantity - :quantity = 0 THEN 'SOLD_OUT' ELSE 'PARTIALLY_BOOKED' END,
        t.bookingReference = :bookingRef,
        t.bookedBy = :bookedBy,
        t.idempotencyKey = :idempotencyKey,
        t.bookedAt = CURRENT_TIMESTAMP
    WHERE t.id = :id AND t.availableQuantity >= :quantity
""")
int bookTicketAtomically(@Param("id") Long id,
                         @Param("quantity") int quantity,
                         @Param("bookingRef") String bookingRef,
                         @Param("bookedBy") String bookedBy,
                         @Param("idempotencyKey") String idempotencyKey);

@Transactional
public void bookTicket(String eventId, String userId) {
    int updated = ticketRepository.bookTicketAtomically(
        ticketId, 1, generateRef(), userId, generateIdempotencyKey()
    );
    if (updated == 0) {
        throw new ConcurrencyException("Booking failed - no tickets available");
    }
}
```

**Result under 1000 concurrent requests for 1 ticket:**
- Exactly 1 successful booking
- 999 immediate failures (0 rows updated)
- Database state: consistent

**Pros:**
- Single database round-trip
- No locks held
- Highest throughput for simple operations
- No retry logic needed

**Cons:**
- Limited to simple, single-row operations
- Cannot express complex business logic
- Requires careful WHERE clause design
- Less flexible for complex domains

---

### 5. Idempotency Keys

```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
public class Ticket {
    @Column(unique = true)
    private String idempotencyKey;
}

public void bookTicket(String eventId, String userId, String idempotencyKey) {
    // Check if already processed
    if (ticketRepository.existsByIdempotencyKey(idempotencyKey)) {
        return getExistingBooking(idempotencyKey);
    }
    
    // Try atomic insert with idempotency key
    try {
        return bookTicketAtomically(eventId, userId, idempotencyKey);
    } catch (DataIntegrityViolationException e) {
        // Another request won the race
        return getExistingBooking(idempotencyKey);
    }
}
```

**Result under 1000 concurrent requests for 1 ticket (with same idempotency key):**
- Exactly 1 successful booking
- 999 return existing booking (deduplicated)
- Database state: consistent

**Pros:**
- Client-controlled deduplication
- Works across retries, network failures, load balancers
- Enables exactly-once semantics
- No server-side locking

**Cons:**
- Requires client cooperation (must generate unique keys)
- Storage overhead for keys
- Key management complexity
- Doesn't prevent concurrent *different* operations on same resource

---

### 6. Redis Distributed Lock (Redlock)

```java
public class RedisDistributedLock implements ConcurrencyStrategy {
    private final StringRedisTemplate redis;
    private final int lockTimeoutMs = 5000;
    
    public <T> T execute(Callable<T> task) {
        String lockKey = "lock:" + task.hashCode();
        String lockValue = UUID.randomUUID().toString();
        
        boolean acquired = redis.opsForValue()
            .setIfAbsent(lockKey, lockValue, Duration.ofMillis(lockTimeoutMs));
        
        if (!acquired) {
            throw new LockAcquisitionException("Could not acquire lock");
        }
        
        try {
            return task.call();
        } finally {
            // Safe release: only delete if we own it
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redis.execute(new DefaultRedisScript<>(script, Long.class), List.of(lockKey), lockValue);
        }
    }
}
```

**Result under 1000 concurrent requests for 1 ticket:**
- Exactly 1 successful booking (if lock works correctly)
- 999 fail to acquire lock
- Database state: consistent

**Pros:**
- Works across multiple service instances
- No database lock contention
- Fine-grained lock control

**Cons:**
- Additional infrastructure (Redis)
- Redlock algorithm has safety concerns (Martin Kleppmann critique)
- Clock synchronization issues
- Network latency adds overhead
- Single point of failure (Redis)

---

## Quantitative Comparison

| Metric | None | Optimistic | Pessimistic | Atomic | Idempotency | Redis Lock |
|--------|------|------------|-------------|--------|-------------|------------|
| **Correctness** | ❌ | ✅ | ✅ | ✅ | ✅ | ✅* |
| **Throughput (1 ticket, 1000 req)** | 1000* | ~50 | ~10 | ~100 | ~100 | ~80 |
| **Latency (p99)** | Low | Medium | High | Low | Low | Medium |
| **Complexity** | Low | Medium | Low | Low | Medium | High |
| **Infrastructure** | None | DB only | DB only | DB only | DB only | Redis |
| **Deadlock Risk** | None | Low | High | None | Low | Medium |
| **Retry Needed** | No | Yes | No | No | Yes (client) | No |

*Incorrect results
*Redlock has known safety issues

## Recommendations

### For Ticket Booking / Inventory
1. **Atomic Update** - Best for simple decrement operations
2. **Optimistic Locking** - When business logic is complex
3. **Idempotency Keys** - When clients can provide keys (API consumers)

### For Financial Transactions
1. **Pessimistic Locking** - Strong consistency required
2. **Optimistic with Retry** - If contention is low

### For Distributed Systems
1. **Idempotency Keys** - Client-side deduplication
2. **Redis Lock** - Cross-service coordination (with caveats)

### Anti-Patterns to Avoid
- ❌ Application-level `synchronized` (doesn't work across instances)
- ❌ `SELECT` then `UPDATE` without locking (race condition)
- ❌ Unbounded retry loops (can cause thundering herd)
- ❌ Redis Redlock for critical data (use single Redis with Lua script instead)

## Testing Methodology

Each strategy is tested with:
1. **Correctness Test** - 1 ticket, 1000 concurrent requests → exactly 1 success
2. **Performance Test** - 100 tickets, 10000 requests → measure throughput/latency
3. **Contention Test** - Vary concurrent users (10, 100, 1000) → measure degradation
4. **Failure Test** - Kill process mid-transaction → verify consistency