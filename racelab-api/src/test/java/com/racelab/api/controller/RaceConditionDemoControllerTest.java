package com.racelab.api.controller;

import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.engine.demo.RaceConditionDemoService;
import com.racelab.engine.dto.RaceConditionDemoRequest;
import com.racelab.engine.dto.RaceConditionDemoResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RaceConditionDemoController.
 */
@ExtendWith(MockitoExtension.class)
class RaceConditionDemoControllerTest {

    @Mock
    private RaceConditionDemoService demoService;

    private RaceConditionDemoController createController() {
        return new RaceConditionDemoController(demoService);
    }

    @Test
    void runDemo_ValidRequest_ShouldReturnResult() {
        RaceConditionDemoRequest request = RaceConditionDemoRequest.builder()
            .eventId("test-event")
            .totalTickets(1)
            .concurrentRequests(100)
            .quantityPerRequest(1)
            .build();

        RaceConditionDemoResult mockResult = RaceConditionDemoResult.builder()
            .eventId("test-event")
            .totalTickets(1)
            .concurrentRequests(100)
            .build();

        when(demoService.runDemo(any(RaceConditionDemoRequest.class))).thenReturn(mockResult);

        RaceConditionDemoController controller = createController();
        ResponseEntity<RaceConditionDemoResult> response = controller.runDemo(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(mockResult);
    }

    @Test
    void runVulnerable_ShouldReturnVulnerableResult() {
        RaceConditionDemoResult.VulnerableResult mockVulnerable = RaceConditionDemoResult.VulnerableResult.builder()
            .successfulBookings(50)
            .failedBookings(50)
            .finalAvailableQuantity(-49)
            .finalBookedQuantity(50)
            .oversold(true)
            .build();

        RaceConditionDemoResult mockResult = RaceConditionDemoResult.builder()
            .vulnerableResult(mockVulnerable)
            .build();

        when(demoService.runDemo(any(RaceConditionDemoRequest.class))).thenReturn(mockResult);

        RaceConditionDemoController controller = createController();
        ResponseEntity<RaceConditionDemoResult.VulnerableResult> response = 
            controller.runVulnerable("test-event", 1, 100, 1);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(mockVulnerable);
    }

    @Test
    void runStrategy_ShouldReturnStrategyResult() {
        RaceConditionDemoResult.StrategyResult mockStrategy = RaceConditionDemoResult.StrategyResult.builder()
            .strategyType(ConcurrencyStrategyType.OPTIMISTIC_LOCKING)
            .successfulBookings(1)
            .failedBookings(99)
            .consistent(true)
            .build();

        RaceConditionDemoResult mockResult = RaceConditionDemoResult.builder()
            .strategyResults(Map.of(ConcurrencyStrategyType.OPTIMISTIC_LOCKING, mockStrategy))
            .build();

        when(demoService.runDemo(any(RaceConditionDemoRequest.class))).thenReturn(mockResult);

        RaceConditionDemoController controller = createController();
        ResponseEntity<RaceConditionDemoResult.StrategyResult> response = 
            controller.runStrategy("test-event", ConcurrencyStrategyType.OPTIMISTIC_LOCKING, 1, 100, 1);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(mockStrategy);
    }

    @Test
    void getStrategies_ShouldReturnAvailableStrategies() {
        RaceConditionDemoController controller = createController();
        ResponseEntity<List<String>> response = controller.getStrategies();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains(
            "OPTIMISTIC_LOCKING",
            "PESSIMISTIC_LOCKING",
            "ATOMIC_UPDATE",
            "IDEMPOTENCY_KEY"
        );
        assertThat(response.getBody()).doesNotContain("NONE", "REDIS_DISTRIBUTED_LOCK");
    }
}