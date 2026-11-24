# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project structure with modular monolith architecture
- racelab-core: Domain models (TestScenario, TestExecution, TestResult, RequestResult, Ticket, Account)
- racelab-core: Repository interfaces with Spring Data JPA
- racelab-core: Concurrency strategy interfaces (6 strategies)
- racelab-core: Invariant detection framework
- racelab-core: HdrHistogram-based percentile calculator
- racelab-core: Cancellation token and concurrency utilities
- racelab-core: Flyway database migrations (V1__initial_schema.sql)
- racelab-engine: ConcurrencyTestRunner with virtual thread support
- racelab-engine: Reactive WebClient HTTP client service
- racelab-engine: Thread-safe ResultAggregator with HdrHistogram
- racelab-engine: TestExecutionService and TestScenarioService
- racelab-engine: Engine configuration properties
- racelab-api: REST controllers for scenarios, executions, results
- racelab-api: DTOs with validation
- racelab-api: MapStruct mappers
- racelab-api: Global exception handler
- racelab-api: OpenAPI/Swagger configuration
- racelab-api: Spring Boot 3.2 application entry point
- racelab-web: React 18 + TypeScript + Vite setup
- racelab-web: Tailwind-inspired CSS with CSS variables
- racelab-web: Reusable components (Button, Card, Badge, Input, Select, Table)
- racelab-web: TanStack Query for data fetching
- racelab-web: Dashboard, Scenarios, Executions, ExecutionDetail, Results pages
- racelab-web: Recharts integration for metrics visualization
- Docker Compose with PostgreSQL, Redis, Kafka, Prometheus, Grafana, Mailhog
- GitHub Actions CI/CD pipeline (build, test, security scan, docker build)
- Comprehensive documentation (ARCHITECTURE, CONCURRENCY, TRANSACTIONS, BENCHMARKING, TESTING, API)
- Architecture Decision Records (Virtual Threads, HdrHistogram, Modular Monolith)

### Changed
- N/A (initial release)

### Deprecated
- N/A

### Removed
- N/A

### Fixed
- N/A

### Security
- N/A

## [1.0.0] - TBD

### Added
- First stable release

---

## Release Types

- **Major** - Breaking API changes, significant architecture changes
- **Minor** - New features, backward compatible
- **Patch** - Bug fixes, backward compatible

## Versioning

This project follows [Semantic Versioning](https://semver.org/).

Format: `MAJOR.MINOR.PATCH[-PRERELEASE]`

Examples:
- `1.0.0` - First stable release
- `1.1.0` - New feature, backward compatible
- `1.0.1` - Bug fix
- `2.0.0` - Breaking changes
- `1.0.0-alpha.1` - Pre-release

## Links

- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)