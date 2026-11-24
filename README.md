# RaceLab

> A distributed systems reliability and concurrency testing platform.

RaceLab helps backend engineers discover, reproduce, analyze, and prevent concurrency bugs in APIs and database-backed systems.

## What RaceLab Does

RaceLab provides a platform for:

1. **Concurrency Testing** - Generate controlled concurrent load against APIs
2. **Race Condition Detection** - Identify duplicate operations, lost updates, overselling
3. **Strategy Comparison** - Compare optimistic locking, pessimistic locking, atomic updates, idempotency keys
4. **Transaction Analysis** - Test isolation levels, dirty reads, phantom reads, deadlocks
5. **Distributed Systems Testing** - Kafka/Redis integration, failure injection
6. **Performance Analytics** - Accurate percentiles (p50, p95, p99), throughput, error rates
7. **Failure Injection** - Latency injection, error simulation, chaos engineering

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      RaceLab Platform                        │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │  Core    │  │ Engine   │  │  API     │  │  Web     │     │
│  │ Domain   │  │ Runner   │  │  REST    │  │  React   │     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │
├─────────────────────────────────────────────────────────────┤
│  PostgreSQL  │  Redis  │  Kafka  │  Prometheus  │  Grafana  │
└─────────────────────────────────────────────────────────────┘
```

## Technology Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Build | Maven (multi-module) |
| Database | PostgreSQL 16 + Flyway |
| Caching | Redis 7 |
| Messaging | Apache Kafka |
| Testing | Testcontainers, JUnit 5, Awaitility |
| Metrics | HdrHistogram, Micrometer, Prometheus |
| Frontend | React 18 + TypeScript + Vite |
| CI/CD | GitHub Actions |
| Observability | OpenTelemetry, Prometheus, Grafana |

## Prerequisites

- **Java 21** (Temurin/OpenJDK)
- **Maven 3.9+**
- **Docker & Docker Compose** (for infrastructure)
- **Node.js 20+** (for frontend)

## Quick Start

### 1. Start Infrastructure

```bash
docker compose up -d postgres
```

### 2. Build and Run Backend

```bash
# Build all modules
mvn clean install -DskipTests

# Run the API module
cd racelab-api
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### 3. Run Frontend (separate terminal)

```bash
cd racelab-web
npm install
npm run dev
```

The UI will be available at `http://localhost:5173`

### 4. Access Swagger UI

Open `http://localhost:8080/swagger-ui.html`

## Running Tests

```bash
# Unit tests only (fast)
mvn test

# Integration tests (requires PostgreSQL)
mvn verify -DskipUnitTests=true

# All tests
mvn verify

# With Testcontainers (auto-starts PostgreSQL)
mvn verify -Pintegration-tests
```

## Creating a Test Scenario

```bash
curl -X POST http://localhost:8080/api/scenarios \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ticket-booking-test",
    "description": "Test concurrent ticket booking",
    "concurrentUsers": 100,
    "totalRequests": 1000,
    "targetEndpoint": "http://localhost:8080/api/tickets/book",
    "httpMethod": "POST",
    "requestPayload": "{\"eventId\": \"concert-2024\", \"quantity\": 1}",
    "concurrencyStrategy": "NONE"
  }'
```

## Running a Test

```bash
# Start execution (async)
curl -X POST http://localhost:8080/api/tests/1/start

# Or synchronous (for testing)
curl -X POST http://localhost:8080/api/tests/1/start-sync
```

## Viewing Results

```bash
# Get execution status
curl http://localhost:8080/api/tests/1

# Get detailed results
curl http://localhost:8080/api/results/execution/1
```

## Example: Ticket Booking Race Condition

RaceLab includes a built-in ticket booking demonstration:

1. Create 1 ticket with 1 available seat
2. Send 1000 concurrent booking requests
3. **Expected**: Exactly 1 successful booking
4. **Vulnerable implementation**: Multiple successful bookings (race condition)
5. **Fixed implementations**: Compare strategies

```bash
# Create scenario with vulnerable strategy
curl -X POST http://localhost:8080/api/scenarios \
  -d '{"name": "vulnerable-booking", "concurrencyStrategy": "NONE", ...}'

# Create scenario with optimistic locking
curl -X POST http://localhost:8080/api/scenarios \
  -d '{"name": "optimistic-booking", "concurrencyStrategy": "OPTIMISTIC_LOCKING", ...}'

# Create scenario with atomic update
curl -X POST http://localhost:8080/api/scenarios \
  -d '{"name": "atomic-booking", "concurrencyStrategy": "ATOMIC_UPDATE", ...}'
```

## Concurrency Strategies Compared

| Strategy | Mechanism | Pros | Cons |
|----------|-----------|------|------|
| **None** | No protection | Baseline | Data corruption |
| **Optimistic Locking** | JPA `@Version` + retry | No locks, good for low contention | Retry overhead, starvation risk |
| **Pessimistic Locking** | `SELECT FOR UPDATE` | Strong consistency | Lock contention, deadlocks |
| **Atomic Update** | Conditional `UPDATE` | Single statement, fast | Limited to simple operations |
| **Idempotency Key** | Client-provided keys | Works across retries, distributed | Requires client cooperation |
| **Redis Distributed Lock** | Redlock algorithm | Cross-service coordination | Extra infrastructure, complexity |

## Project Structure

```
RaceLab/
├── pom.xml                          # Parent POM
├── racelab-core/                    # Domain models, repositories, interfaces
├── racelab-engine/                  # Concurrency test runner, HTTP client, aggregation
├── racelab-api/                     # REST API, controllers, DTOs, OpenAPI
├── racelab-web/                     # React frontend
├── docker-compose.yml               # Local infrastructure
├── prometheus.yml                   # Prometheus config
├── .github/workflows/ci.yml         # CI/CD pipeline
└── docs/                            # Documentation
```

## Configuration

Key environment variables:

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
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Concurrency Strategies](docs/CONCURRENCY.md)
- [Transaction Isolation](docs/TRANSACTIONS.md)
- [Benchmarking Methodology](docs/BENCHMARKING.md)
- [Testing Guide](docs/TESTING.md)
- [API Reference](docs/API.md)
- [Architecture Decisions](docs/DECISIONS/)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

MIT License - see [LICENSE](LICENSE) for details.

## Status

**Phase A (Concurrency Engine)**: In Progress
- [x] Project structure & build config
- [x] Core domain models
- [x] Test scenario/execution entities
- [x] Concurrency test runner
- [x] Result aggregation with HdrHistogram
- [x] REST API with OpenAPI
- [x] Docker Compose infrastructure
- [x] CI/CD pipeline
- [ ] Frontend dashboard
- [ ] Integration tests

---

Built with ❤️ for engineers who care about correctness.