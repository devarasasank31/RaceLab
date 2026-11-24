# Database Transaction Isolation Experiments

This document describes the transaction isolation level experiments implemented in RaceLab.

## Isolation Levels

### PostgreSQL Isolation Levels

| Level | Dirty Read | Non-Repeatable Read | Phantom Read | Serialization Anomaly |
|-------|------------|---------------------|--------------|----------------------|
| READ UNCOMMITTED | Possible | Possible | Possible | Possible |
| READ COMMITTED | Prevented | Possible | Possible | Possible |
| REPEATABLE READ | Prevented | Prevented | Possible* | Possible |
| SERIALIZABLE | Prevented | Prevented | Prevented | Prevented |

*PostgreSQL prevents phantom reads in REPEATABLE READ via predicate locking.

### Default in PostgreSQL
- **READ COMMITTED** - Default isolation level

## Experiments

### 1. Dirty Read (READ UNCOMMITTED)

**Scenario:** Transaction A updates a row, Transaction B reads it before A commits.

```sql
-- Session A
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
-- Don't commit yet

-- Session B (READ UNCOMMITTED)
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
SELECT balance FROM accounts WHERE id = 1; -- Sees uncommitted -100
```

**PostgreSQL Behavior:** READ UNCOMMITTED behaves as READ COMMITTED (dirty reads prevented).

### 2. Non-Repeatable Read (READ COMMITTED)

**Scenario:** Transaction A reads a row, Transaction B updates and commits, Transaction A reads again - sees different value.

```sql
-- Session A (READ COMMITTED)
BEGIN;
SELECT balance FROM accounts WHERE id = 1; -- Returns 1000

-- Session B
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
COMMIT;

-- Session A (same transaction)
SELECT balance FROM accounts WHERE id = 1; -- Returns 900 (different!)
COMMIT;
```

**RaceLab Test:** Concurrent read/update with READ COMMITTED isolation.

### 3. Phantom Read (REPEATABLE READ)

**Scenario:** Transaction A reads a range, Transaction B inserts a row in that range and commits, Transaction A reads range again - sees new row.

```sql
-- Session A (REPEATABLE READ)
BEGIN;
SELECT * FROM tickets WHERE event_id = 'concert'; -- Returns 0 rows

-- Session B
BEGIN;
INSERT INTO tickets (event_id, available_quantity) VALUES ('concert', 100);
COMMIT;

-- Session A
SELECT * FROM tickets WHERE event_id = 'concert'; -- Returns 1 row (phantom!)
COMMIT;
```

**PostgreSQL Behavior:** REPEATABLE READ prevents phantoms via predicate locking (Serializable Snapshot Isolation).

### 4. Lost Update (All Levels)

**Scenario:** Two transactions read same value, both increment, both write - one increment lost.

```sql
-- Session A
BEGIN;
SELECT balance FROM accounts WHERE id = 1; -- 1000

-- Session B
BEGIN;
SELECT balance FROM accounts WHERE id = 1; -- 1000

-- Session A
UPDATE accounts SET balance = 1100 WHERE id = 1; -- 1000 + 100
COMMIT;

-- Session B
UPDATE accounts SET balance = 1050 WHERE id = 1; -- 1000 + 50 (loses A's 100!)
COMMIT;
```

**Prevention:** Requires explicit locking or application-level concurrency control.

### 5. Serialization Anomaly (SERIALIZABLE)

**Scenario:** Two transactions that are individually correct but produce incorrect result when interleaved.

```sql
-- Session A
BEGIN ISOLATION LEVEL SERIALIZABLE;
-- Check: no overlapping bookings for room 101 at 10:00
SELECT count(*) FROM bookings WHERE room = 101 AND time = '10:00';
-- Returns 0
INSERT INTO bookings (room, time, user) VALUES (101, '10:00', 'Alice');
COMMIT;

-- Session B (concurrent)
BEGIN ISOLATION LEVEL SERIALIZABLE;
SELECT count(*) FROM bookings WHERE room = 101 AND time = '10:00';
-- Returns 0 (doesn't see A's uncommitted insert)
INSERT INTO bookings (room, time, user) VALUES (101, '10:00', 'Bob');
-- ERROR: could not serialize access due to concurrent update
COMMIT;
```

## RaceLab Implementation

### Transaction Isolation Test Service

```java
@Service
@RequiredArgsConstructor
public class TransactionIsolationTestService {

    private final EntityManager entityManager;
    
    public void testReadCommittedNonRepeatableRead() {
        // Execute in READ COMMITTED (default)
        // Thread 1: Read account balance
        // Thread 2: Update and commit
        // Thread 1: Read again → different value
    }
    
    public void testRepeatableReadPhantomPrevention() {
        // Execute in REPEATABLE READ
        // Thread 1: SELECT count(*) FROM tickets WHERE event_id = 'x'
        // Thread 2: INSERT new ticket for event 'x' and commit
        // Thread 1: SELECT count(*) again → same count (no phantom)
    }
    
    public void testSerializableSerializationFailure() {
        // Execute in SERIALIZABLE
        // Two concurrent transactions checking/inserting same constraint
        // One should fail with SQLState 40001 (serialization_failure)
    }
    
    public void testLostUpdatePrevention() {
        // Test various strategies:
        // 1. No protection → lost update
        // 2. Optimistic locking (@Version) → retry
        // 3. Pessimistic locking (SELECT FOR UPDATE) → block
        // 4. Atomic UPDATE → single statement
    }
}
```

### Running Experiments

```bash
# Start test with specific isolation level
curl -X POST http://localhost:8080/api/experiments/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "isolationLevel": "READ_COMMITTED",
    "scenario": "NON_REPEATABLE_READ",
    "concurrentTransactions": 100
  }'
```

## Lock Analysis

### PostgreSQL Lock Types

```sql
-- View active locks
SELECT * FROM pg_locks WHERE pid = pg_backend_pid();

-- Lock modes (weakest to strongest)
1. ACCESS SHARE (SELECT)
2. ROW SHARE (SELECT FOR SHARE)
3. ROW EXCLUSIVE (INSERT/UPDATE/DELETE)
4. SHARE UPDATE EXCLUSIVE (VACUUM, CREATE INDEX CONCURRENTLY)
5. SHARE (CREATE INDEX)
6. SHARE ROW EXCLUSIVE (trigger)
7. EXCLUSIVE (TRUNCATE)
8. ACCESS EXCLUSIVE (ALTER TABLE, DROP TABLE)
```

### Lock Contention Test

```java
@Test
void testLockContention() {
    // Spawn 100 threads
    // Each tries to update same row with SELECT FOR UPDATE
    // Measure: wait time, throughput, deadlocks
}
```

## Deadlock Detection

### Common Deadlock Pattern

```
Thread 1: Lock Account A → Lock Account B
Thread 2: Lock Account B → Lock Account A
```

### Prevention
1. **Consistent lock ordering** - Always lock accounts by ID order
2. **Lock timeouts** - `SET lock_timeout = '5s'`
3. **Deadlock detection** - PostgreSQL auto-detects and aborts one transaction

### RaceLab Deadlock Test

```java
@Test
void testDeadlockDetection() {
    // Two threads transfer between same accounts in opposite directions
    // Verify one succeeds, one gets deadlock exception (SQLState 40P01)
}
```

## Best Practices

1. **Use READ COMMITTED** as default - good balance of consistency/performance
2. **Use REPEATABLE READ** for reporting/analytics queries
3. **Use SERIALIZABLE** only when serialization anomalies are unacceptable
4. **Always handle serialization failures** (SQLState 40001) with retry
5. **Keep transactions short** - minimize lock holding time
6. **Order locks consistently** - prevent deadlocks
7. **Use advisory locks** for application-level coordination