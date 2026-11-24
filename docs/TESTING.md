# Testing Guide

This document describes the testing strategy, tools, and practices for RaceLab.

## Test Pyramid

```
         ┌─────────────┐
         │   E2E /     │  Few - Critical user journeys
         │ Integration │
        ┌───────────────┐
        │  Integration  │  Many - Database, external services
        │   Tests       │
       ┌─────────────────┐
       │   Unit Tests    │  Many - Pure logic, fast, isolated
       └─────────────────┘
```

## Test Categories

### 1. Unit Tests (`*Test.java`)

**Location:** `src/test/java` alongside source

**Characteristics:**
- No Spring context (`@ExtendWith(MockitoExtension.class)`)
- No database, no network
- Test pure logic: services, utilities, mappers
- Fast (< 100ms per test class)
- Deterministic

**Examples:**
- `PercentileCalculatorTest` - Verify HdrHistogram calculations
- `CancellationTokenTest` - Cancellation behavior
- `ConcurrencyUtilsTest` - Thread pool creation, retry logic
- `TestScenarioValidationTest` - Domain validation rules

**Naming Convention:** `<ClassName>Test`

### 2. Integration Tests (`*IntegrationTest.java`)

**Location:** `src/test/java` (or `src/integration-test/java`)

**Characteristics:**
- Full Spring context (`@SpringBootTest`)
- Testcontainers for PostgreSQL, Redis, Kafka
- Test real repository implementations
- Test controller endpoints (`@AutoConfigureMockMvc` or `@AutoConfigureWebTestClient`)
- Slower (seconds per class)

**Examples:**
- `TestScenarioRepositoryIntegrationTest` - JPA mappings, queries
- `ConcurrencyTestRunnerIntegrationTest` - Full execution flow
- `TicketBookingIntegrationTest` - Race condition scenarios
- `TransactionIsolationIntegrationTest` - DB isolation levels

**Naming Convention:** `<ClassName>IntegrationTest`

### 3. Concurrency Tests (`*ConcurrencyTest.java`)

**Location:** `src/test/java`

**Characteristics:**
- Specifically test concurrent behavior
- Use `ExecutorService`, `CountDownLatch`, `CyclicBarrier`
- Run multiple iterations to catch flakiness
- Verify invariants after concurrent execution
- May use `@RepeatedTest` or custom repetition

**Examples:**
- `OptimisticLockingConcurrencyTest` - Verify retry behavior
- `AtomicUpdateConcurrencyTest` - Verify exactly-once
- `DuplicateBookingConcurrencyTest` - Verify no duplicates
- `LostUpdateConcurrencyTest` - Verify balance consistency

**Naming Convention:** `<Strategy>ConcurrencyTest`

## Test Infrastructure

### Testcontainers

```java
@Testcontainers
class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("racelab")
        .withUsername("racelab")
        .withPassword("racelab")
        .withReuse(true); // Reuse across test classes

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

### Concurrency Test Utilities

```java
public final class ConcurrencyTestUtils {

    public static void runConcurrently(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    task.run();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        if (!errors.isEmpty()) {
            throw new AssertionError("Concurrent execution failed", errors.get(0));
        }
    }

    public static <T> List<T> runConcurrently(int threadCount, Callable<T> task) throws InterruptedException {
        List<T> results = Collections.synchronizedList(new ArrayList<>());
        runConcurrently(threadCount, () -> {
            try {
                results.add(task.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return results;
    }
}
```

## Running Tests

### Maven Commands

```bash
# Unit tests only (fast, no Docker needed)
mvn test

# Integration tests (requires Docker)
mvn verify -DskipUnitTests=true

# All tests
mvn verify

# Specific test class
mvn test -Dtest=PercentileCalculatorTest

# Specific integration test
mvn verify -Dtest=ConcurrencyTestRunnerIntegrationTest -DskipUnitTests=true

# Run with specific profile
mvn verify -Pintegration-tests

# Skip tests (not recommended for CI)
mvn clean install -DskipTests
```

### IDE Configuration

**IntelliJ IDEA:**
1. Enable "Delegate IDE build/run actions to Maven"
2. Configure Testcontainers: Settings → Build → Maven → Runner → VM Options: `-Dtestcontainers.reuse.enable=true`
3. Run configurations: Add `-Dspring.profiles.active=test`

**VS Code:**
1. Install "Extension Pack for Java"
2. Configure `settings.json`:
```json
{
  "java.test.config": "src/test/resources/application-test.yml",
  "maven.terminal.useJavaHome": true
}
```

## Test Data Management

### Test Fixtures

```java
@TestComponent
public class TestFixtures {

    public TestScenario createTicketBookingScenario() {
        return TestScenario.builder()
            .name("ticket-booking-" + UUID.randomUUID())
            .concurrentUsers(100)
            .totalRequests(1000)
            .targetEndpoint("http://localhost:8080/api/tickets/book")
            .httpMethod(HttpMethod.POST)
            .requestPayload("{\"eventId\": \"test-event\", \"quantity\": 1}")
            .concurrencyStrategy(ConcurrencyStrategyType.NONE)
            .build();
    }

    public Ticket createTicket(String eventId, int quantity) {
        return Ticket.builder()
            .eventId(eventId)
            .eventName("Test Event")
            .totalQuantity(quantity)
            .availableQuantity(quantity)
            .status(TicketStatus.AVAILABLE)
            .build();
    }
}
```

### Database Cleanup

```java
@AfterEach
void cleanup() {
    // Delete in reverse dependency order
    requestResultRepository.deleteAllInBatch();
    testResultRepository.deleteAllInBatch();
    testExecutionRepository.deleteAllInBatch();
    testScenarioRepository.deleteAllInBatch();
    ticketRepository.deleteAllInBatch();
    accountRepository.deleteAllInBatch();
}
```

## Concurrency Test Patterns

### Pattern 1: Verify Exactly-Once

```java
@Test
void exactlyOneBookingWithAtomicUpdate() {
    // Given: 1 ticket, 1000 concurrent requests
    Ticket ticket = ticketRepository.save(createTicket("event-1", 1));
    
    // When: Execute concurrently
    List<BookingResult> results = ConcurrencyTestUtils.runConcurrently(1000, () -> 
        bookingService.bookAtomic("event-1", "user-" + ThreadLocalRandom.current().nextInt())
    );
    
    // Then: Exactly 1 success
    long successCount = results.stream().filter(BookingResult::isSuccess).count();
    assertThat(successCount).isEqualTo(1);
    
    // And: Database state consistent
    Ticket finalTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
    assertThat(finalTicket.getAvailableQuantity()).isEqualTo(0);
    assertThat(finalTicket.getBookedQuantity()).isEqualTo(1);
}
```

### Pattern 2: Verify Retry Behavior

```java
@Test
void optimisticLockingRetriesOnConflict() {
    // Given: 1 ticket, optimistic locking
    Ticket ticket = ticketRepository.save(createTicket("event-1", 1));
    
    // When: Execute with optimistic locking
    int attempts = AtomicInteger::new();
    List<BookingResult> results = ConcurrencyTestUtils.runConcurrently(100, () -> {
        attempts.incrementAndGet();
        return bookingService.bookOptimistic("event-1", "user");
    });
    
    // Then: 1 success, retries occurred
    assertThat(results).filteredOn(BookingResult::isSuccess).hasSize(1);
    assertThat(attempts.get()).isGreaterThan(100); // Retries happened
}
```

### Pattern 3: Verify No Lost Updates

```java
@Test
void noLostUpdatesWithPessimisticLocking() {
    // Given: Account with 1000 balance
    Account account = accountRepository.save(createAccount("acc-1", BigDecimal.valueOf(1000)));
    
    // When: 100 concurrent deposits of 10
    List<TransferResult> results = ConcurrencyTestUtils.runConcurrently(100, () -> 
        accountService.depositPessimistic("acc-1", BigDecimal.valueOf(10))
    );
    
    // Then: All succeeded, final balance = 2000
    assertThat(results).allMatch(TransferResult::isSuccess);
    Account finalAccount = accountRepository.findById(account.getId()).orElseThrow();
    assertThat(finalAccount.getBalance()).isEqualByComparingTo("2000");
}
```

## Flaky Test Prevention

1. **Use proper synchronization** - `CountDownLatch`, `CyclicBarrier`, `CompletableFuture`
2. **Adequate timeouts** - 30s minimum for concurrency tests
3. **Deterministic test data** - No random IDs in assertions
4. **Isolated test databases** - Testcontainers per test class or method
5. **Retry only for known flaky operations** - Not for masking bugs
6. **Run multiple iterations** - `@RepeatedTest(5)` for concurrency tests

## CI/CD Integration

### GitHub Actions

```yaml
# .github/workflows/ci.yml
- name: Run unit tests
  run: mvn test

- name: Run integration tests
  run: mvn verify -DskipUnitTests=true
  env:
    TESTCONTAINERS_RYUK_DISABLED: true
    DOCKER_HOST: unix:///var/run/docker.sock
```

### Test Reports

- **Surefire** - Unit test reports (`target/surefire-reports/`)
- **Failsafe** - Integration test reports (`target/failsafe-reports/`)
- **JaCoCo** - Coverage reports (`target/site/jacoco/`)

### Quality Gates

- Unit test coverage > 80%
- Integration test coverage > 60%
- No flaky tests (fail build on flakiness)
- All concurrency tests pass 5 consecutive runs