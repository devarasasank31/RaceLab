package com.racelab.engine.benchmark;

import com.racelab.core.domain.ConcurrencyStrategyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for BenchmarkRunner.
 */
@Testcontainers
@SpringBootTest
class BenchmarkRunnerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("racelab")
        .withUsername("racelab")
        .withPassword("racelab")
        .withReuse(true);

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withReuse(true);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.redis.host", REDIS::getHost);
        registry.add("spring.redis.port", REDIS::getFirstMappedPort);
    }

    @Autowired
    private BenchmarkRunner benchmarkRunner;

    @Test
    void runBenchmark_QuickRun_ReturnsResults() {
        // Given
        BenchmarkRunner.BenchmarkConfig config = BenchmarkRunner.BenchmarkConfig.builder()
            .name("integration-test")
            .eventId("benchmark-test")
            .totalTickets(1)
            .concurrentRequests(50)
            .quantityPerRequest(1)
            .iterations(2)
            .strategies(List.of(
                ConcurrencyStrategyType.OPTIMISTIC_LOCKING,
                ConcurrencyStrategyType.ATOMIC_UPDATE
            ))
            .build();

        // When
        BenchmarkRunner.BenchmarkResult result = benchmarkRunner.runBenchmark(config);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("integration-test");
        assertThat(result.getResults()).hasSize(2);

        for (BenchmarkRunner.StrategyBenchmarkResult strategyResult : result.getResults()) {
            assertThat(strategyResult.getIterations()).hasSize(2);
            
            for (BenchmarkRunner.IterationResult iteration : strategyResult.getIterations()) {
                assertThat(iteration.isCompleted()).isTrue();
                assertThat(iteration.getSuccesses() + iteration.getFailures()).isEqualTo(50);
                assertThat(iteration.getSuccesses()).isEqualTo(1);
                assertThat(iteration.getFailures()).isEqualTo(49);
                assertThat(iteration.isConsistent()).isTrue();
            }

            // Aggregate metrics
            assertThat(strategyResult.getAvgSuccessRate()).isEqualTo(2.0); // 1/50 * 100
            assertThat(strategyResult.getTotalSuccesses()).isEqualTo(2); // 1 per iteration * 2 iterations
        }
    }
}