# RaceLab Project Audit

**Date:** 2026-11-24  
**Auditor:** Senior Engineering Team  
**Repository:** https://github.com/devarasasank31/RaceLab.git  
**Branch:** main (empty)

---

## 1. Current Repository State

### What Exists
- **Empty Git repository** - no commits, no files
- **Remote configured** - origin points to https://github.com/devarasasank31/RaceLab.git
- **No source code, no build files, no documentation**

### What Works
- Nothing - this is a greenfield project

### What Is Broken
- N/A - no code exists

### Technical Weaknesses Identified
- No project structure
- No build configuration (Maven/Gradle)
- No Spring Boot application
- No database configuration
- No testing infrastructure
- No CI/CD pipeline
- No frontend

---

## 2. Proposed Architecture

### Modular Monolith Structure
```
racelab/
├── racelab-core/           # Core domain models, interfaces, shared utilities
├── racelab-engine/         # Concurrency testing engine (Phase A)
├── racelab-detection/      # Race condition detection (Phase B)
├── racelab-strategies/     # Concurrency strategy implementations (Phase C)
├── racelab-transactions/   # Transaction analysis (Phase D)
├── racelab-distributed/    # Distributed systems testing (Phase E)
├── racelab-analytics/      # Performance analytics (Phase F)
├── racelab-failure/        # Failure injection (Phase G)
├── racelab-investigation/  # AI investigation (Phase H - optional)
├── racelab-api/            # REST controllers, DTOs, OpenAPI
├── racelab-web/            # React/TypeScript frontend
└── racelab-integration/    # Testcontainers, integration test utilities
```

### Technology Stack (Per Specification)
- **Java 21**, **Spring Boot 3.x**, **Maven**
- **PostgreSQL** + **Flyway** migrations
- **Redis** (where justified for distributed locking)
- **Apache Kafka** (where justified for event-driven testing)
- **Testcontainers** for integration tests
- **React + TypeScript** frontend
- **Docker + Docker Compose** for local development
- **GitHub Actions** for CI/CD
- **OpenTelemetry + Prometheus + Grafana** for observability

---

## 3. Implementation Sequence (Phased Approach)

### Phase A: Concurrency Testing Engine (Week 1-2)
**Priority: CRITICAL - Foundation for everything else**

1. **Project Setup**
   - Multi-module Maven project
   - Spring Boot 3.x parent POM
   - PostgreSQL + Flyway configuration
   - Testcontainers setup
   - Basic CI/CD pipeline

2. **Core Domain**
   - `TestScenario` entity and repository
   - `TestExecution` entity with status, timestamps, configuration
   - `TestResult` entity with aggregated metrics
   - `RequestResult` entity for individual request tracking

3. **Concurrency Engine**
   - `ConcurrencyTestRunner` using `ExecutorService` + `CompletableFuture`
   - Configurable: concurrent users, request count, duration, ramp-up
   - HTTP method, endpoint, headers, payload configuration
   - Thread-safe result aggregation with `LongAdder`, `DoubleAdder`
   - Graceful cancellation via `CancellationToken` pattern
   - Bounded thread pools with `ThreadPoolExecutor`

4. **API Layer**
   - REST endpoints for scenario CRUD and execution control
   - Input validation with Spring Validation
   - OpenAPI/Swagger documentation

5. **Tests**
   - Unit tests for engine logic
   - Integration tests with Testcontainers (PostgreSQL)
   - Concurrency tests verifying thread-safety

### Phase B: Race Condition Detection (Week 2-3)
**Priority: HIGH - Core value proposition**

1. **Ticket Booking Demo**
   - `Ticket` entity with version/lock fields
   - `BookingService` with vulnerable implementation
   - `BookingServiceFixed` with corrected implementations
   - Scenario: 1 ticket, 1000 concurrent requests → exactly 1 success

2. **Detection Framework**
   - `InvariantChecker` interface
   - Built-in checkers: duplicate booking, lost update, overselling, balance inconsistency
   - Evidence collection for violations

3. **Vulnerable vs Fixed Comparisons**
   - No protection (baseline)
   - Optimistic locking (`@Version`)
   - Pessimistic locking (`SELECT FOR UPDATE`)
   - Atomic updates (`UPDATE ... SET count = count - 1 WHERE count > 0`)
   - Idempotency keys

### Phase C: Concurrency Strategy Comparison (Week 3-4)
**Priority: HIGH - Differentiator**

1. **Strategy Interface**
   - `ConcurrencyStrategy` with `execute(Callable<T>)`
   - Implementations for each approach
   - Benchmarking harness

2. **Comparison API**
   - Run same scenario with multiple strategies
   - Side-by-side results
   - Latency, throughput, correctness metrics

3. **Documentation**
   - `docs/CONCURRENCY.md` with trade-off analysis

### Phase D: Database Transaction Analysis (Week 4-5)
**Priority: MEDIUM - Educational value**

1. **Transaction Experiments**
   - Isolation level demonstrations (READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE)
   - Dirty read, non-repeatable read, phantom read scenarios
   - Deadlock detection and resolution
   - Lock contention measurement

2. **PostgreSQL-Specific Tests**
   - Advisory locks
   - `pg_locks` inspection
   - Transaction log analysis

### Phase E: Distributed Systems Testing (Week 5-6)
**Priority: MEDIUM - Advanced features**

1. **Redis Integration**
   - Distributed locking (Redlock algorithm)
   - Rate limiting
   - Cache invalidation testing

2. **Kafka Integration**
   - Event producers/consumers
   - Duplicate event simulation
   - Idempotent consumer patterns
   - Dead letter queue handling

3. **Failure Simulation**
   - Network partition simulation
   - Consumer failure/restart
   - Message replay

### Phase F: Performance Analytics (Week 6-7)
**Priority: HIGH - Required for credible results**

1. **Metrics Calculation**
   - Percentiles (p50, p95, p99) using HdrHistogram or t-digest
   - Throughput (requests/second)
   - Error rate breakdown
   - Latency distribution histograms

2. **Reporting**
   - JSON/CSV export
   - Real-time streaming updates via WebSocket
   - Comparison reports

### Phase G: Failure Injection (Week 7-8)
**Priority: MEDIUM - Chaos engineering**

1. **Failure Types**
   - Latency injection (fixed, random, percentile-based)
   - Error injection (HTTP 5xx, timeouts, connection resets)
   - Resource exhaustion (connection pool, thread pool)
   - Clock skew simulation

2. **Safety**
   - Isolated test environments only
   - Automatic cleanup
   - No production impact possible

### Phase H: AI Investigation (Week 8+)
**Priority: LOW - Optional enhancement**

1. **Evidence-Based Analysis**
   - Test report ingestion
   - Invariant violation correlation
   - Log/metric pattern matching
   - Root cause hypothesis generation
   - Regression test suggestion

2. **Optional LLM Integration**
   - Pluggable provider interface
   - Works without API keys (rule-based fallback)

---

## 4. Dependencies to Introduce

### Core (Required Immediately)
```xml
<dependencies>
    <!-- Spring Boot 3.x -->
    <dependency>spring-boot-starter-web</dependency>
    <dependency>spring-boot-starter-data-jpa</dependency>
    <dependency>spring-boot-starter-validation</dependency>
    <dependency>spring-boot-starter-actuator</dependency>
    <dependency>spring-boot-starter-security</dependency> <!-- for auth later -->
    
    <!-- Database -->
    <dependency>postgresql</dependency>
    <dependency>flyway-core</dependency>
    
    <!-- Concurrency -->
    <dependency>HdrHistogram</dependency> <!-- for accurate percentiles -->
    
    <!-- Testing -->
    <dependency>spring-boot-starter-test</dependency>
    <dependency>testcontainers-postgresql</dependency>
    <dependency>testcontainers-junit-jupiter</dependency>
    <dependency>awaitility</dependency>
    
    <!-- Utilities -->
    <dependency>lombok</dependency>
    <dependency>mapstruct</dependency>
</dependencies>
```

### Phase B-C (Add When Needed)
```xml
<dependency>spring-boot-starter-data-redis</dependency>
<dependency>lettuce-core</dependency>
```

### Phase E (Add When Needed)
```xml
<dependency>spring-kafka</dependency>
<dependency>testcontainers-kafka</dependency>
<dependency>testcontainers-redis</dependency>
```

### Phase H (Optional)
```xml
<dependency>spring-ai-openai</dependency> <!-- or other provider -->
```

### Frontend (Phase A Parallel)
- React 18 + TypeScript + Vite
- Tailwind CSS or plain CSS modules
- Chart.js or Recharts for visualizations
- React Query / TanStack Query for data fetching

---

## 5. Immediate Next Steps

1. **Initialize multi-module Maven project** with parent POM
2. **Create core module** with domain entities and repositories
3. **Set up PostgreSQL + Flyway** with initial schema
4. **Implement ConcurrencyTestRunner** (Phase A core)
5. **Build REST API** for test management
6. **Write first integration tests** with Testcontainers
7. **Configure GitHub Actions** CI pipeline
8. **Create Docker Compose** for local development
9. **Initialize React frontend** in `racelab-web`

---

## 6. Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Thread pool exhaustion | Bounded queues, rejection handlers, circuit breakers |
| Database connection leaks | HikariCP configuration, connection validation, leak detection |
| Flaky concurrency tests | Deterministic test scenarios, controlled execution, retry logic |
| Percentile calculation errors | Use HdrHistogram, verify against known datasets |
| Testcontainers resource conflicts | Unique ports, proper cleanup, Ryuk disable option |
| AI hallucination in Phase H | Evidence-only mode, explicit "insufficient evidence" responses |

---

## 7. Success Criteria (Reiterated from Spec)

- [ ] Application starts successfully
- [ ] Database migrations execute correctly
- [ ] Docker Compose works
- [ ] APIs work end to end
- [ ] Concurrent requests can be generated
- [ ] Race conditions can be reproduced
- [ ] Corrected implementations pass invariants
- [ ] Concurrency strategies can be compared
- [ ] Database transactions are tested
- [ ] Performance metrics are calculated correctly
- [ ] Results persist across application restarts
- [ ] Failure experiments work safely
- [ ] Frontend displays actual backend results
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Documentation matches implementation
- [ ] GitHub Actions works
- [ ] Repository contains no committed secrets
- [ ] Git history contains meaningful development work
- [ ] A new developer can clone and run the project