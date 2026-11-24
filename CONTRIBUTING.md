# Contributing to RaceLab

Thank you for your interest in contributing to RaceLab! This document provides guidelines for contributing to the project.

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). Please read it before contributing.

## How to Contribute

### Reporting Bugs

1. Check existing issues first
2. Create a new issue with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (Java version, OS, Docker version)
   - Relevant logs/stack traces

### Suggesting Features

1. Check existing issues and discussions
2. Create a feature request issue with:
   - Clear problem statement
   - Proposed solution
   - Alternatives considered
   - Implementation approach (if known)

### Pull Requests

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes following the guidelines below
4. Run tests: `mvn verify`
5. Push to your fork
6. Create a Pull Request against `main`

## Development Setup

### Prerequisites
- Java 21 (Temurin recommended)
- Maven 3.9+
- Docker & Docker Compose
- Node.js 20+ (for frontend)
- Git

### Initial Setup
```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/RaceLab.git
cd RaceLab

# Add upstream remote
git remote add upstream https://github.com/devarasasank31/RaceLab.git

# Start infrastructure
docker compose up -d postgres

# Build backend
mvn clean install -DskipTests

# Build frontend
cd racelab-web && npm ci && cd ..

# Run backend
cd racelab-api && mvn spring-boot:run

# Run frontend (separate terminal)
cd racelab-web && npm run dev
```

## Code Standards

### Java
- Java 21 language features encouraged
- Constructor injection only
- Records for DTOs and immutable data
- Lombok: `@RequiredArgsConstructor`, `@Value`, `@Builder` (on DTOs)
- MapStruct for entity↔DTO mapping
- No raw types, no field injection
- Spotless formatting (Google Java Format)

### Testing
- Unit tests: `@ExtendWith(MockitoExtension.class)`, no Spring context
- Integration tests: `@SpringBootTest` + Testcontainers
- Concurrency tests: Multiple iterations, proper synchronization
- Naming: `<MethodName>_<Scenario>_<ExpectedOutcome>`

### Git Commits
```
<type>(<module>): <concise summary>

<optional body>

<optional footer>
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `perf`, `chore`, `build`

Examples:
```
feat(engine): add configurable concurrency test runner
fix(detection): resolve lost update in optimistic locking test
docs(api): add OpenAPI examples for test execution endpoints
test(integration): add Testcontainers PostgreSQL setup
```

### Branch Naming
- Features: `feature/<phase>-<description>` (e.g., `feature/phase-a-concurrency-engine`)
- Bug fixes: `fix/<description>`
- Documentation: `docs/<description>`
- Refactoring: `refactor/<description>`

## Pull Request Process

1. **Title**: Clear, concise, follows commit format
2. **Description**: 
   - What changed and why
   - Link to related issue
   - Screenshots (for UI changes)
3. **Tests**: All tests pass, new tests for new functionality
4. **Documentation**: Updated if API or behavior changed
5. **Review**: At least one approval required
6. **Merge**: Squash and merge to `main`

## Project Structure

```
RaceLab/
├── pom.xml                    # Parent POM
├── racelab-core/              # Domain, repositories, interfaces
├── racelab-engine/            # Concurrency engine
├── racelab-api/               # REST API
├── racelab-web/               # React frontend
├── docker-compose.yml         # Local infrastructure
├── .github/workflows/         # CI/CD
└── docs/                      # Documentation
```

## Testing Requirements

### Before Submitting PR
```bash
# Format check
mvn spotless:check

# Unit tests
mvn test

# Integration tests (requires Docker)
mvn verify -DskipUnitTests=true

# Full build
mvn verify
```

### Test Categories
- **Unit** (`*Test.java`): Pure logic, no Spring, fast
- **Integration** (`*IntegrationTest.java`): Spring + Testcontainers
- **Concurrency** (`*ConcurrencyTest.java`): Multi-threaded scenarios

## Documentation

Update relevant documentation:
- `README.md` - Quick start, overview
- `docs/ARCHITECTURE.md` - Architecture changes
- `docs/CONCURRENCY.md` - New strategies
- `docs/TESTING.md` - Test patterns
- `docs/API.md` - API changes
- `docs/DECISIONS/` - New ADRs for significant decisions

## Release Process

Maintainers handle releases:
1. Update version in `pom.xml`
2. Update `CHANGELOG.md`
3. Create git tag: `git tag -a v1.0.0 -m "Release 1.0.0"`
4. Push tag: `git push origin v1.0.0`
5. GitHub Actions builds and publishes artifacts

## Getting Help

- **GitHub Discussions**: Questions, ideas, general discussion
- **GitHub Issues**: Bug reports, feature requests
- **Documentation**: Check `docs/` directory first

## Recognition

Contributors are recognized in:
- `CONTRIBUTORS.md` (auto-generated)
- Release notes
- Project README

Thank you for contributing to RaceLab!