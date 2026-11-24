package com.racelab.engine.integration;

import com.racelab.core.domain.TestScenario;
import com.racelab.core.domain.HttpMethod;
import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.core.repository.TestScenarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TestScenarioRepository.
 */
@Testcontainers
@DataJpaTest
@Import(TestScenarioRepository.class)
class TestScenarioRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("racelab")
        .withUsername("racelab")
        .withPassword("racelab")
        .withReuse(true);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private TestScenarioRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveAndFind_ShouldPersistScenario() {
        TestScenario scenario = TestScenario.builder()
            .name("integration-test")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .concurrencyStrategy(ConcurrencyStrategyType.NONE)
            .build();

        TestScenario saved = repository.save(scenario);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0);

        Optional<TestScenario> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("integration-test");
    }

    @Test
    void findByName_ShouldReturnScenario() {
        TestScenario scenario = TestScenario.builder()
            .name("unique-name")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        repository.save(scenario);
        entityManager.flush();

        Optional<TestScenario> found = repository.findByName("unique-name");

        assertThat(found).isPresent();
        assertThat(found.get().getConcurrentUsers()).isEqualTo(10);
    }

    @Test
    void searchByName_ShouldReturnMatchingScenarios() {
        repository.save(TestScenario.builder().name("test-scenario-1").concurrentUsers(10).totalRequests(100).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build());
        repository.save(TestScenario.builder().name("test-scenario-2").concurrentUsers(10).totalRequests(100).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build());
        repository.save(TestScenario.builder().name("other-scenario").concurrentUsers(10).totalRequests(100).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build());
        entityManager.flush();

        List<TestScenario> results = repository.searchByName("test-scenario");

        assertThat(results).hasSize(2);
    }

    @Test
    void findAllOrderByCreatedAtDesc_ShouldReturnOrdered() {
        TestScenario s1 = TestScenario.builder().name("first").concurrentUsers(10).totalRequests(100).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build();
        TestScenario s2 = TestScenario.builder().name("second").concurrentUsers(10).totalRequests(100).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build();
        TestScenario s3 = TestScenario.builder().name("third").concurrentUsers(10).totalRequests(100).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build();

        repository.save(s1);
        repository.save(s2);
        repository.save(s3);
        entityManager.flush();

        List<TestScenario> results = repository.findAllOrderByCreatedAtDesc();

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getName()).isEqualTo("third");
        assertThat(results.get(1).getName()).isEqualTo("second");
        assertThat(results.get(2).getName()).isEqualTo("first");
    }
}