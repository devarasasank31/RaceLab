package com.racelab.api.controller;

import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.engine.benchmark.BenchmarkRunner;
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
 * Unit tests for BenchmarkController.
 */
@ExtendWith(MockitoExtension.class)
class BenchmarkControllerTest {

    @Mock
    private BenchmarkRunner benchmarkRunner;

    private BenchmarkController createController() {
        return new BenchmarkController(benchmarkRunner);
    }

    @Test
    void runBenchmark_ValidConfig_ShouldReturnResult() {
        BenchmarkRunner.BenchmarkConfig config = BenchmarkRunner.BenchmarkConfig.builder()
            .name("test-benchmark")
            .eventId("test-event")
            .totalTickets(1)
            .concurrentRequests(100)
            .quantityPerRequest(1)
            .iterations(3)
            .strategies(List.of(
                ConcurrencyStrategyType.OPTIMISTIC_LOCKING,
                ConcurrencyStrategyType.ATOMIC_UPDATE
            ))
            .build();

        BenchmarkRunner.BenchmarkResult mockResult = BenchmarkRunner.BenchmarkResult.builder()
            .name("test-benchmark")
            .build();

        when(benchmarkRunner.runBenchmark(any(BenchmarkRunner.BenchmarkConfig.class))).thenReturn(mockResult);

        BenchmarkController controller = new BenchmarkController(benchmarkRunner);
        ResponseEntity<BenchmarkRunner.BenchmarkResult> response = controller.runBenchmark(config);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(mockResult);
    }

    @Test
    void runQuickBenchmark_ShouldReturnResult() {
        BenchmarkRunner.BenchmarkResult mockResult = BenchmarkRunner.BenchmarkResult.builder()
            .name("quick-benchmark")
            .build();

        when(benchmarkRunner.runBenchmark(any(BenchmarkRunner.BenchmarkConfig.class))).thenReturn(mockResult);

        BenchmarkController controller = new BenchmarkController(benchmarkRunner);
        ResponseEntity<BenchmarkRunner.BenchmarkResult> response = controller.runQuickBenchmark(
            "quick-test", "test-event", 1, 50, 1, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(mockResult);
    }

    @Test
    void getStrategies_ShouldReturnAvailableStrategies() {
        BenchmarkController controller = new BenchmarkController(benchmarkRunner);
        ResponseEntity<List<String>> response = controller.getStrategies();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains(
            "OPTIMISTIC_LOCKING",
            "PESSIMISTIC_LOCKING",
            "ATOMIC_UPDATE",
            "IDEMPOTENCY_KEY",
            "REDIS_DISTRIBUTED_LOCK"
        );
        assertThat(response.getBody()).doesNotContain("NONE");
    }
}