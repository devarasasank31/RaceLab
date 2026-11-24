package com.racelab.engine.demo;

import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.engine.dto.RaceConditionDemoRequest;
import com.racelab.engine.dto.RaceConditionDemoResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for race condition demo service.
 */
@Testcontainers
@SpringBootTest
class RaceConditionDemoServiceIntegrationTest {

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
    private RaceConditionDemoService demoService;

    @Test
    void runDemo_OneTicket100Requests_ReturnsComparisonResults() {
        // Given
        RaceConditionDemoRequest request = RaceConditionDemoRequest.builder()
            .eventId("integration-test-event")
            .totalTickets(1)
            .concurrentRequests(100)
            .quantityPerRequest(1)
            .strategies(List.of(
                ConcurrencyStrategyType.OPTIMISTIC_LOCKING,
                ConcurrencyStrategyType.PESSIMISTIC_LOCKING,
                ConcurrencyStrategyType.ATOMIC_UPDATE
            ))
            .build();

        // When
        RaceConditionDemoResult result = demoService.runDemo(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isEqualTo("integration-test-event");
        assertThat(result.getTotalTickets()).isEqualTo(1);
        assertThat(result.getConcurrentRequests()).isEqualTo(100);
        assertThat(result.getVulnerableResult()).isNotNull();
        assertThat(result.getStrategyResults()).hasSize(3);

        // Vulnerable test should show race condition (or at least complete)
        RaceConditionDemoResult.VulnerableResult vulnerable = result.getVulnerableResult();
        assertThat(vulnerable.getSuccessfulBookings() + vulnerable.getFailedBookings()).isEqualTo(100);

        // Strategy results
        for (var entry : result.getStrategyResults().entrySet()) {
            RaceConditionDemoResult.StrategyResult strategy = entry.getValue();
            assertThat(strategy.getSuccessfulBookings() + strategy.getFailedBookings()).isEqualTo(100);
            assertThat(strategy.getSuccessfulBookings()).isEqualTo(1);
            assertThat(strategy.isConsistent()).isTrue();
            assertThat(strategy.isOversold()).isFalse();
        }

        // Summary
        assertThat(result.getSummary()).isNotNull();
        assertThat(result.getSummary().getConsistentStrategies()).containsExactlyInAnyOrder(
            "OPTIMISTIC_LOCKING", "PESSIMISTIC_LOCKING", "ATOMIC_UPDATE"
        );
        assertThat(result.getSummary().getOversoldStrategies()).isEmpty();
    }

    @Test
    void runDemo_MultipleTickets_AllBookedCorrectly() {
        // Given
        RaceConditionDemoRequest request = RaceConditionDemoRequest.builder()
            .eventId("multi-ticket-test")
            .totalTickets(5)
            .concurrentRequests(200)
            .quantityPerRequest(1)
            .strategies(List.of(
                ConcurrencyStrategyType.OPTIMISTIC_LOCKING,
                ConcurrencyStrategyType.ATOMIC_UPDATE
            ))
            .build();

        // When
        RaceConditionDemoResult result = demoService.runDemo(request);

        // Then
        assertThat(result.getTotalTickets()).isEqualTo(5);
        assertThat(result.getConcurrentRequests()).isEqualTo(200);

        for (var entry : result.getStrategyResults().entrySet()) {
            RaceConditionDemoResult.StrategyResult strategy = entry.getValue();
            assertThat(strategy.getSuccessfulBookings()).isEqualTo(5);
            assertThat(strategy.getFailedBookings()).isEqualTo(195);
            assertThat(strategy.isConsistent()).isTrue();
            assertThat(strategy.isOversold()).isFalse();
        }
    }
}