# ADR 003: Modular Monolith Architecture

## Status
Accepted

## Context

RaceLab needs to balance:
- **Development velocity** - Single deployable, easy refactoring
- **Domain separation** - Clear boundaries between concerns
- **Future extractability** - Potential to split services if needed
- **Operational simplicity** - Single deployment, shared database

Microservices introduce:
- Network latency and failure modes
- Distributed transaction complexity
- Operational overhead (multiple deployments, service discovery)
- Data consistency challenges

## Decision

Implement RaceLab as a **modular monolith** with:
- **Maven multi-module** project structure
- **Clear module boundaries** with explicit dependencies
- **Shared database** (single PostgreSQL instance)
- **Single deployable artifact** (Spring Boot JAR)
- **Domain-driven module design**

## Module Structure

```
racelab-parent (pom)
├── racelab-core         # Domain, repositories, interfaces (NO Spring)
├── racelab-engine       # Concurrency engine, HTTP client, aggregation
├── racelab-api          # REST controllers, DTOs, OpenAPI
└── racelab-web          # React frontend (separate npm project)
```

### Dependency Rules

```
racelab-api  ──▶ racelab-engine ──▶ racelab-core
     │                │
     └────────────────┘ (transitive)
```

- **racelab-core**: Zero Spring dependencies, pure Java
- **racelab-engine**: Spring Boot (WebFlux, Actuator), depends on core
- **racelab-api**: Spring Boot (Web, Security, OpenAPI), depends on core + engine
- **No circular dependencies** enforced by Maven

## Consequences

### Positive
- **Simple Deployment**: Single JAR, single database
- **Fast Development**: No network calls between modules, easy debugging
- **Refactoring Safety**: IDE-supported cross-module refactoring
- **Transaction Simplicity**: Single database = ACID transactions across domains
- **Testability**: Unit test core in isolation, integration test full stack
- **Extractable Later**: Module boundaries ready for service extraction

### Negative
- **Single Failure Domain**: Bug in one module can crash entire app
- **Scaling**: Must scale entire app, not individual modules
- **Technology Lock-in**: All modules share Java/Spring version
- **Team Coupling**: Requires coordination for shared modules

## Module Boundaries

### racelab-core
**Owns**: Domain entities, repository interfaces, strategy interfaces, detection interfaces, utilities, Flyway migrations
**Exposes**: Public API via interfaces and domain objects
**No Spring**: Pure Java, only Jakarta Validation, JPA annotations

### racelab-engine
**Owns**: Test execution logic, HTTP client implementations, result aggregation, execution/services
**Exposes**: Service interfaces (TestExecutionService, TestScenarioService)
**Depends On**: racelab-core

### racelab-api
**Owns**: REST controllers, DTOs, mappers, exception handling, OpenAPI config, security config
**Exposes**: HTTP endpoints
**Depends On**: racelab-core, racelab-engine

## Future Extraction Criteria

Consider extracting a module as a service when:
1. **Independent scaling needed** - Different load profiles
2. **Different release cadence** - Module changes frequently
3. **Team ownership** - Dedicated team for module
4. **Technology divergence** - Different language/framework needed
5. **Failure isolation** - Critical module needs separate failure domain

## Migration Path (if needed)

1. **Add API gateway** - Route to modules via internal HTTP
2. **Extract module** - Move to separate repo, deploy as service
3. **Database per service** - Split schema, use events for sync
4. **Update clients** - Point to new service URLs
5. **Monitor** - Observe latency, errors, consistency

## References

- [Modular Monolith by Simon Brown](https://simonbrown.je/modular-monolith/)
- [Spring Modulith](https://github.com/spring-projects/spring-modulith)
- [Martin Fowler on MonolithFirst](https://martinfowler.com/bliki/MonolithFirst.html)