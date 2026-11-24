# RaceLab Development Instructions

This document provides project-specific guidance for AI agents and human developers working on RaceLab.

---

## Project Overview

**RaceLab** is a distributed systems reliability and concurrency testing platform. It helps backend engineers discover, reproduce, analyze, and prevent concurrency bugs in APIs and database-backed systems.

**Repository:** https://github.com/devarasasank31/RaceLab.git  
**Main Branch:** `main`  
**Java Version:** 21  
**Build Tool:** Maven (multi-module)  
**Framework:** Spring Boot 3.x

---

## Development Workflow

### Branch Strategy
- `main` - Protected, deployable state only
- Feature branches: `feature/<phase>-<description>` (e.g., `feature/phase-a-concurrency-engine`)
- Bug fixes: `fix/<description>`
- Documentation: `docs/<description>`

### Commit Standards
```
<type>(<module>): <concise summary>

<optional body with context>

<optional footer with issue references>
```

**Types:** feat, fix, docs, refactor, test, perf, chore, build

**Examples:**
```
feat(engine): add configurable concurrency test runner
fix(detection): resolve lost update in optimistic locking test
docs(api): add OpenAPI examples for test execution endpoints
test(integration): add Testcontainers PostgreSQL setup
```

### Pull Request Process
1. Create feature branch from `main`
2. Implement changes with tests
3. Run full test suite locally (`mvn verify`)
4. Push branch and create PR
5. CI must pass (compile, unit tests, integration tests, formatting)
6. Code review required
7. Squash merge to `main`

---

## Code Standards

### Java Conventions
- **Java 21** language features encouraged (records, pattern matching, virtual threads where appropriate)
- **Constructor injection** only - no field injection
- **Records** for DTOs and immutable data carriers
- **Sealed interfaces** for strategy patterns
- **No raw types** - always parameterize generics
- **No `@Autowired` on fields** - use constructor injection
- **Lombok** permitted: `@RequiredArgsConstructor`, `@Value`, `@Data` (on DTOs only), `@Builder`
- **MapStruct** for entity↔DTO mapping

### Package Structure
```
com.racelab.<module>
├── domain          # Entities, value objects, domain events
├── repository      # Spring Data JPA repositories
├── service         # Business logic, orchestration
├── api             # Controllers, DTOs, mappers
├── config          # Configuration classes
├── exception       # Custom exceptions, handlers
└── util            # Pure utility functions
```

### Naming Conventions
| Element | Convention | Example |
|---------|------------|---------|
| Packages | lowercase, singular | `com.racelab.engine.runner` |
| Classes/Interfaces | PascalCase | `ConcurrencyTestRunner` |
| Records | PascalCase | `TestScenarioRequest` |
| Methods | camelCase | `executeTestScenario` |
| Constants | UPPER_SNAKE_CASE | `MAX_CONCURRENT_USERS` |
| Enum values | UPPER_SNAKE_CASE | `RUNNING, COMPLETED, FAILED` |
| Database tables | snake_case plural | `test_scenarios` |
| Database columns | snake_case | `concurrent_users` |

### Exception Handling
- **Domain exceptions** extend `RaceLabException` (unchecked)
- **API layer** maps to consistent error response:
  ```json
  {
    "timestamp": "2026-11-24T10:30:00Z",
    "status": 400,
    "error": "Validation Failed",
    "message": "concurrentUsers must be between 1 and 10000",
    "path": "/api/tests"
  }
  ```
- **Never swallow exceptions** - log and rethrow or handle explicitly
- **Use `@ControllerAdvice`** for global exception handling

### Concurrency Guidelines
- **Bounded thread pools** - always configure `corePoolSize`, `maxPoolSize`, `queueCapacity`
- **Virtual threads** (Java 21) for I/O-bound work: `Executors.newVirtualThreadPerTaskExecutor()`
- **Platform threads** for CPU-bound work: fixed thread pool
- **Thread-safe aggregation** - `LongAdder`, `DoubleAdder`, `ConcurrentHashMap`, `AtomicReference`
- **Cancellation** - use `CompletableFuture` with `cancel(true)` or custom `CancellationToken`
- **No `synchronized` on public methods** - use `ReentrantLock` or `StampedLock` if needed
- **No `ThreadLocal` abuse** - prefer context propagation via parameters

### Database Conventions
- **Flyway migrations** in `src/main/resources/db/migration/V<version>__<description>.sql`
- **Naming:** `V1__initial_schema.sql`, `V2__add_index_on_execution_status.sql`
- **Always specify schema** in migration files
- **Indexes** for query patterns - document why in migration comment
- **Soft deletes** - `deleted_at` timestamp, not hard deletes
- **Audit columns** - `created_at`, `updated_at` on all entities

### Testing Standards
- **Test naming:** `<MethodName>_<Scenario>_<ExpectedOutcome>`
  - `executeTestScenario_WithValidConfig_ReturnsExecutionId`
  - `executeTestScenario_WithZeroUsers_ThrowsValidationException`
- **Arrange-Act-Assert** structure
- **Testcontainers** for all integration tests (PostgreSQL, Redis, Kafka)
- **No `@SpringBootTest` for unit tests** - use `@ExtendWith(MockitoExtension.class)`
- **Concurrency tests** must run multiple iterations to catch flakiness
- **Percentile verification** - use known datasets to verify HdrHistogram calculations

---

## Module Guidelines

### racelab-core
- **No Spring dependencies** - pure Java domain model
- **Entities, value objects, enums, domain exceptions**
- **Shared interfaces** (e.g., `ConcurrencyStrategy`, `InvariantChecker`)
- **Utility classes** with no external dependencies

### racelab-engine
- **Core concurrency testing logic**
- `ConcurrencyTestRunner`, `TestExecutor`, `ResultAggregator`
- **No HTTP client code** - delegate to `HttpClientService` interface
- **Configurable via properties** - `racelab.engine.*`

### racelab-api
- **Controllers, DTOs, Mappers, OpenAPI config**
- **Validation annotations** on all request DTOs
- **No business logic** - delegate to services
- **Global exception handler** here

### racelab-web (Frontend)
- **React 18 + TypeScript + Vite**
- **Feature-based structure:** `src/features/<feature>/`
- **API layer:** `src/api/` with generated types from OpenAPI
- **State:** TanStack Query for server state, React Context for UI state
- **Charts:** Recharts or Chart.js
- **Styling:** Tailwind CSS
- **No direct DOM manipulation**

---

## Configuration Management

### Application Properties Priority
1. `application.yml` (committed, non-secret defaults)
2. `application-local.yml` (gitignored, developer overrides)
3. Environment variables (CI/CD, production)
4. `@TestPropertySource` for tests

### Required Environment Variables
```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=racelab
POSTGRES_USER=racelab
POSTGRES_PASSWORD=racelab

# Redis (Phase C+)
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka (Phase E+)
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Optional: AI Investigation (Phase H)
OPENAI_API_KEY=  # Only if using OpenAI provider
```

### Docker Compose Services
- `postgres` - PostgreSQL 16
- `redis` - Redis 7 (Phase C+)
- `kafka` - Kafka + Zookeeper (Phase E+)
- `prometheus` - Metrics collection
- `grafana` - Dashboards
- `mailhog` - Email testing (if needed)

---

## Build & Test Commands

### Maven Commands
```bash
# Full build with all tests
mvn clean verify

# Unit tests only (fast)
mvn test

# Integration tests only (requires Docker)
mvn verify -DskipUnitTests=true

# Specific module
mvn test -pl racelab-engine

# Skip tests (not recommended for CI)
mvn clean install -DskipTests

# Format check
mvn spotless:check

# Apply formatting
mvn spotless:apply

# Dependency analysis
mvn dependency:analyze
mvn versions:display-dependency-updates
```

### Docker Commands
```bash
# Start infrastructure
docker compose up -d postgres

# Start all
docker compose up -d

# View logs
docker compose logs -f postgres

# Stop and remove volumes
docker compose down -v
```

### Frontend Commands (in `racelab-web/`)
```bash
# Install dependencies
npm ci

# Dev server
npm run dev

# Build
npm run build

# Lint
npm run lint

# Test
npm run test

# Type check
npm run typecheck
```

---

## CI/CD Pipeline (GitHub Actions)

### Required Workflows
1. **`ci.yml`** - On every push/PR:
   - Checkout
   - Setup Java 21 (Temurin)
   - Setup Node.js 20 (for frontend)
   - Cache Maven (~/.m2) and npm (~/.npm)
   - `mvn verify` (unit + integration tests with Testcontainers)
   - `npm ci && npm run build && npm run lint` (frontend)
   - Spotless check
   - Upload test reports

2. **`release.yml`** - On tag push (`v*`):
   - Build artifacts
   - Publish to GitHub Packages / Maven Central
   - Docker image build + push
   - Generate changelog

### Testcontainers in CI
- Use `testcontainers.docker.socketOverride` for Docker-in-Docker
- Prefer GitHub Actions `ubuntu-latest` with Docker pre-installed
- Set `TESTCONTAINERS_RYUK_DISABLED=true` if Ryuk causes issues

---

## Documentation Standards

### Required Files (All in Repository Root)
| File | Purpose |
|------|---------|
| `README.md` | Project overview, quick start, architecture diagram |
| `AGENTS.md` | This file - development instructions |
| `PROJECT_AUDIT.md` | Audit findings, architecture, roadmap |
| `ARCHITECTURE.md` | Detailed architecture, design decisions |
| `CONTRIBUTING.md` | Contribution guidelines |
| `CHANGELOG.md` | Keep a Changelog format |
| `docs/CONCURRENCY.md` | Concurrency strategy deep-dive |
| `docs/TRANSACTIONS.md` | Transaction isolation experiments |
| `docs/BENCHMARKING.md` | Methodology, percentile calculation |
| `docs/TESTING.md` | Test strategy, how to run tests |
| `docs/API.md` | OpenAPI reference, examples |
| `docs/DECISIONS/` | ADR files (e.g., `001-use-virtual-threads.md`) |

### ADR Format
```markdown
# ADR 001: Use Virtual Threads for I/O-Bound Concurrency

## Status
Accepted

## Context
We need to handle 10,000+ concurrent HTTP requests in tests...

## Decision
Use Java 21 virtual threads via `Executors.newVirtualThreadPerTaskExecutor()`...

## Consequences
- Pros: Lightweight, scales to millions, simple code
- Cons: Not suitable for CPU-bound work, requires JDK 21+
```

---

## Security Checklist (Pre-Commit)

- [ ] No hardcoded passwords, API keys, tokens
- [ ] No `.env` files committed
- [ ] No `application-prod.yml` with real credentials
- [ ] No `console.log` / `System.out.println` in production code
- [ ] No `TODO: remove before commit` comments
- [ ] Dependencies scanned: `mvn org.owasp:dependency-check-maven:check`
- [ ] Secrets scan: `gitleaks detect --source .`

---

## Performance Testing Checklist

Before claiming performance results:
- [ ] Warm-up runs executed (JIT compilation)
- [ ] GC pauses measured and reported
- [ ] Percentiles calculated from raw data (HdrHistogram), not averages
- [ ] Test duration ≥ 30 seconds for steady-state
- [ ] Concurrent user ramp-up documented
- [ ] Hardware/container specs recorded
- [ ] Database connection pool sized appropriately
- [ ] Network latency between client/server measured

---

## Debugging Common Issues

| Issue | Likely Cause | Solution |
|-------|--------------|----------|
| Testcontainers fails to start | Docker not running / socket permissions | Start Docker Desktop; `chmod 666 /var/run/docker.sock` |
| Port conflicts in parallel tests | Fixed ports in config | Use dynamic ports: `server.port=0` |
| Flaky concurrency tests | Non-deterministic timing | Add `awaitility` with proper conditions; increase timeouts |
| OOM in test runs | Thread pool too large / leaks | Reduce pool size; check `ThreadMXBean` for leaks |
| Flyway migration fails | Checksum mismatch | `mvn flyway:repair` locally; never repair in CI |
| Redis connection refused | Redis not started | `docker compose up -d redis` |
| Kafka `LEADER_NOT_AVAILABLE` | Topic not created | Auto-create topics or create in `@BeforeAll` |

---

## Phase-Specific Instructions

### Phase A: Concurrency Engine
- **Focus:** Correctness over features
- **Key classes:** `ConcurrencyTestRunner`, `TestExecutor`, `ResultAggregator`
- **Test target:** 10,000 concurrent virtual threads, 1M requests without OOM
- **Metrics:** Must use HdrHistogram from day one

### Phase B: Race Detection
- **Focus:** Reproducible demonstrations
- **Each scenario:** Vulnerable version → test fails → Fixed version → test passes
- **Evidence:** Capture actual database state, not just HTTP responses

### Phase C: Strategy Comparison
- **Focus:** Fair comparison
- **Same workload** across all strategies
- **Document trade-offs** honestly - no "best" strategy

### Phase D: Transactions
- **Focus:** PostgreSQL-specific behavior
- **Use `pg_locks`, `pg_stat_activity`** for evidence
- **Test isolation levels** with actual concurrent transactions

### Phase E: Distributed
- **Focus:** Real failure modes
- **Testcontainers** for Redis/Kafka
- **Simulate:** network partition, broker failure, consumer crash

### Phase F: Analytics
- **Focus:** Statistical correctness
- **Percentiles from HdrHistogram** - verify against known data
- **Streaming results** via WebSocket for long tests

### Phase G: Failure Injection
- **Focus:** Safety
- **Only in isolated test containers**
- **Automatic cleanup** in `@AfterEach`/`@AfterAll`

### Phase H: AI Investigation
- **Focus:** Evidence-based only
- **Rule-based fallback** when no LLM key
- **Explicit "insufficient evidence"** responses

---

## Quick Reference: Key Files to Create First

```
racelab/
├── pom.xml                          # Parent POM
├── racelab-core/
│   └── pom.xml
├── racelab-engine/
│   └── pom.xml
├── racelab-api/
│   └── pom.xml
├── racelab-web/                     # Frontend (separate npm project)
│   └── package.json
├── docker-compose.yml
├── .github/workflows/ci.yml
├── .gitignore
├── .editorconfig
├── spotless-maven-plugin config (in parent POM)
└── README.md
```

---

## Contact / Escalation

- **Architecture decisions:** Create ADR in `docs/DECISIONS/`
- **Blocking issues:** Open GitHub issue with `blocker` label
- **Security concerns:** Email security@racelab.local (placeholder)
- **Performance regressions:** Compare with `main` baseline in CI

---

*Last Updated: 2026-11-24*  
*Version: 1.0 - Initial project setup*