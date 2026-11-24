# RaceLab Architecture

## Overview

RaceLab is a modular monolith built with Spring Boot 3.x and Java 21, designed for concurrency testing and race condition detection. The architecture emphasizes correctness, observability, and extensibility.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         RaceLab Platform                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐             │
│  │  racelab-   │    │  racelab-   │    │  racelab-   │             │
│  │   core      │◄───│   engine    │◄───│   api       │             │
│  │             │    │             │    │             │             │
│  │ • Domain    │    │ • Runner    │    │ • Controllers│            │
│  │ • Repositories│   │ • HTTP Client│   │ • DTOs       │            │
│  │ • Strategies │    │ • Aggregator │   │ • OpenAPI    │            │
│  │ • Detection  │    │ • Services  │    │ • Validation │            │
│  └─────────────┘    └─────────────┘    └─────────────┘             │
│         ▲                  ▲                  ▲                    │
│         │                  │                  │                    │
│         └──────────────────┼──────────────────┘                    │
│                            │                                       │
│              ┌─────────────┴─────────────┐                         │
│              │   Infrastructure Layer     │                         │
│              │                            │                         │
│              │  PostgreSQL  │  Redis     │                         │
│              │  (Flyway)    │  (Phase C) │                         │
│              │              │  Kafka     │                         │
│              │              │  (Phase E) │                         │
│              └────────────────────────────┘                         │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Module Responsibilities

### racelab-core
**Pure domain layer** - No Spring dependencies
- Domain entities (TestScenario, TestExecution, TestResult, RequestResult, Ticket, Account)
- Repository interfaces (Spring Data JPA)
- Concurrency strategy interfaces
- Invariant detection interfaces
- Utility classes (PercentileCalculator, CancellationToken, ConcurrencyUtils)
- Database migrations (Flyway)
- Domain exceptions

### racelab-engine
**Concurrency testing engine**
- `ConcurrencyTestRunner` - Main orchestration
- `WebClientHttpClientService` - Reactive HTTP client
- `ResultAggregator` - Thread-safe result aggregation with HdrHistogram
- `TestExecutionService` - Execution lifecycle management
- `TestScenarioService` - Scenario CRUD
- Engine configuration properties

### racelab-api
**REST API layer**
- Controllers for scenarios, executions, results
- DTOs with validation
- MapStruct mappers
- Global exception handling
- OpenAPI/Swagger documentation
- Spring Security configuration (future)

## Concurrency Model

### Thread Pools
- **Virtual Threads** (Java 21): Used for I/O-bound HTTP requests via `Executors.newVirtualThreadPerTaskExecutor()`
- **Platform Thread Pool**: Bounded `ThreadPoolExecutor` for CPU-bound work (core=4, max=16, queue=10000)

### Result Aggregation
- `LongAdder` for counters (total, success, failure)
- `ConcurrentHashMap<LongAdder>` for status code and error distributions
- `HdrHistogram` for lock-free latency recording with accurate percentiles

### Cancellation
- `CancellationToken` with `AtomicBoolean` for cooperative cancellation
- Propagated through `CompletableFuture` and executor shutdown

## Data Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│  Controller │────▶│   Service   │────▶│   Runner    │
│  (Browser/  │     │  (API)      │     │  (Engine)   │     │  (Engine)   │
│   curl)     │     │             │     │             │     │             │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                           │                    │                    │
                           ▼                    ▼                    ▼
                    ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
                    │ Validation  │     │ Repository  │     │ HTTP Client │
                    │ (Jakarta)   │     │ (JPA)       │     │ (WebClient) │
                    └─────────────┘     └─────────────┘     └─────────────┘
                                                   │                    │
                                                   ▼                    ▼
                                            ┌─────────────┐     ┌─────────────┐
                                            │ PostgreSQL  │     │  Target     │
                                            │  (Flyway)   │     │  Service    │
                                            └─────────────┘     └─────────────┘
                                                   │
                                                   ▼
                                            ┌─────────────┐
                                            │ Aggregator  │
                                            │ (HdrHist.)  │
                                            └─────────────┘
```

## Database Schema

### Core Tables
- `test_scenarios` - Test configuration
- `test_executions` - Execution instances with status
- `test_results` - Aggregated metrics (one per execution)
- `request_results` - Individual request records

### Demo Tables
- `tickets` - For race condition demonstrations
- `accounts` - For lost update demonstrations

### Indexing Strategy
- Foreign keys indexed for join performance
- Status columns indexed for filtering
- Time-based columns indexed for time-range queries
- Unique constraints on business keys (scenario name, booking reference, idempotency key)

## Concurrency Strategies

| Strategy | Implementation | Use Case |
|----------|----------------|----------|
| NONE | No protection | Baseline comparison |
| OPTIMISTIC_LOCKING | JPA `@Version` + retry | Low contention, high read |
| PESSIMISTIC_LOCKING | `SELECT FOR UPDATE` | High contention, strong consistency |
| ATOMIC_UPDATE | Conditional `UPDATE ... WHERE` | Simple counters, inventory |
| IDEMPOTENCY_KEY | Client-provided keys + unique constraint | Distributed retries, exactly-once |
| REDIS_DISTRIBUTED_LOCK | Redlock algorithm | Cross-service coordination |

## Metrics & Observability

### Application Metrics (Micrometer + Prometheus)
- HTTP request latency, throughput, errors
- JVM metrics (GC, memory, threads)
- Custom business metrics (test executions, success rates)

### Distributed Tracing (OpenTelemetry)
- Trace context propagation across async boundaries
- Span attributes for test execution correlation

### Logging
- Structured JSON logging (production)
- Correlation IDs for request tracing
- Debug-level for concurrency debugging

## Security Considerations

- No authentication in Phase A (local development only)
- Spring Security configured for future phases
- Input validation at API boundary
- SQL injection prevention via JPA parameters
- Rate limiting via infrastructure (future)

## Deployment Architecture

### Local Development
```
docker-compose.yml
├── postgres:16
├── redis:7 (Phase C)
├── kafka:7.5 (Phase E)
├── prometheus
├── grafana
└── mailhog
```

### Production (Future)
- Kubernetes deployment
- PostgreSQL: Cloud SQL / RDS / Patroni cluster
- Redis: Cluster mode
- Kafka: Strimzi operator
- Prometheus/Grafana: Managed or self-hosted
- TLS termination at ingress

## Extensibility Points

1. **Custom Concurrency Strategies** - Implement `ConcurrencyStrategy` interface
2. **Custom Invariant Checkers** - Implement `InvariantChecker` interface
3. **Custom HTTP Clients** - Implement `HttpClientService` interface
4. **Custom Failure Injectors** - Implement `FailureInjector` interface (Phase G)
5. **AI Investigation Providers** - Implement `InvestigationProvider` interface (Phase H)

## Decision Records

See [docs/DECISIONS/](DECISIONS/) for Architecture Decision Records.