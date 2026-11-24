package com.racelab.engine.aggregator;

import com.racelab.core.domain.RequestResult;
import com.racelab.core.domain.TestExecution;
import com.racelab.core.domain.TestResult;
import com.racelab.core.domain.TestScenario;
import com.racelab.core.domain.HttpMethod;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ResultAggregator.
 */
class ResultAggregatorTest {

    private final ResultAggregator aggregator = new ResultAggregator();

    @Test
    void createAggregatedResults_ShouldCreateEmptyResults() {
        ResultAggregator.AggregatedResults results = aggregator.createAggregatedResults();

        assertThat(results.getTotalRequests()).isZero();
        assertThat(results.getSuccessfulRequests()).isZero();
        assertThat(results.getFailedRequests()).isZero();
        assertThat(results.getLatencyHistogram().getTotalCount()).isZero();
    }

    @Test
    void recordSuccess_ShouldIncrementCounters() {
        ResultAggregator.AggregatedResults results = aggregator.createAggregatedResults();

        results.recordSuccess(100, 200, 1024);
        results.recordSuccess(200, 200, 2048);

        assertThat(results.getTotalRequests()).isEqualTo(2);
        assertThat(results.getSuccessfulRequests()).isEqualTo(2);
        assertThat(results.getFailedRequests()).isZero();
        assertThat(results.getTotalResponseBytes()).isEqualTo(3072);
        assertThat(results.getLatencyHistogram().getTotalCount()).isEqualTo(2);
    }

    @Test
    void recordFailure_ShouldIncrementFailureCounters() {
        ResultAggregator.AggregatedResults results = aggregator.createAggregatedResults();

        results.recordFailure(50, 500, "Internal Server Error");
        results.recordFailure(0, 0, "Connection timeout");

        assertThat(results.getTotalRequests()).isEqualTo(2);
        assertThat(results.getSuccessfulRequests()).isZero();
        assertThat(results.getFailedRequests()).isEqualTo(2);
        assertThat(results.getLatencyHistogram().getTotalCount()).isEqualTo(1); // Only one with latency > 0
    }

    @Test
    void recordMixed_ShouldTrackAllMetrics() {
        ResultAggregator.AggregatedResults results = aggregator.createAggregatedResults();

        results.recordSuccess(100, 200, 1000);
        results.recordSuccess(200, 201, 2000);
        results.recordFailure(150, 404, "Not Found");
        results.recordFailure(0, 0, "Timeout");

        assertThat(results.getTotalRequests()).isEqualTo(4);
        assertThat(results.getSuccessfulRequests()).isEqualTo(2);
        assertThat(results.getFailedRequests()).isEqualTo(2);
    }

    @Test
    void statusCodeDistribution_ShouldTrackCorrectly() {
        ResultAggregator.AggregatedResults results = aggregator.createAggregatedResults();

        results.recordSuccess(100, 200, 1000);
        results.recordSuccess(100, 201, 1000);
        results.recordFailure(100, 404, "Not Found");
        results.recordFailure(100, 500, "Server Error");
        results.recordFailure(100, 500, "Server Error");

        Map<Integer, Long> distribution = results.getStatusCodeDistribution();

        assertThat(distribution).containsEntry(200, 1L);
        assertThat(distribution).containsEntry(201, 1L);
        assertThat(distribution).containsEntry(404, 1L);
        assertThat(distribution).containsEntry(500, 2L);
    }

    @Test
    void errorBreakdown_ShouldTrackCorrectly() {
        ResultAggregator.AggregatedResults results = aggregator.createAggregatedResults();

        results.recordFailure(100, 500, "Internal Server Error");
        results.recordFailure(100, 500, "Internal Server Error");
        results.recordFailure(100, 404, "Not Found");

        Map<String, Long> breakdown = results.getErrorBreakdown();

        assertThat(breakdown).containsEntry("Internal Server Error", 2L);
        assertThat(breakdown).containsEntry("Not Found", 1L);
    }

    @Test
    void buildTestResult_ShouldCalculateCorrectMetrics() {
        TestScenario scenario = TestScenario.builder()
            .name("test")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        TestExecution execution = TestExecution.builder()
            .scenario(scenario)
            .build();

        ResultAggregator.AggregatedResults results = aggregator.createAggregatedResults();

        // Record some latencies: 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 ms
        for (int i = 1; i <= 10; i++) {
            results.recordSuccess(i * 10L, 200, 1000);
        }

        TestResult testResult = aggregator.buildTestResult(execution, results);

        assertThat(testResult.getTotalRequests()).isEqualTo(10);
        assertThat(testResult.getSuccessfulRequests()).isEqualTo(10);
        assertThat(testResult.getFailedRequests()).isZero();
        assertThat(testResult.getAvgLatencyMs()).isCloseTo(55.0, within(1.0));
        assertThat(testResult.getMedianLatencyMs()).isCloseTo(55.0, within(5.0));
        assertThat(testResult.getP95LatencyMs()).isCloseTo(95.0, within(5.0));
        assertThat(testResult.getP99LatencyMs()).isCloseTo(99.0, within(5.0));
        assertThat(testResult.getMinLatencyMs()).isCloseTo(10.0, within(1.0));
        assertThat(testResult.getMaxLatencyMs()).isCloseTo(100.0, within(1.0));
        assertThat(testResult.getErrorRatePercent()).isZero();
    }

    @Test
    void updateExecutionCounts_ShouldUpdateExecution() {
        TestScenario scenario = TestScenario.builder()
            .name("test")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        TestExecution execution = TestExecution.builder()
            .scenario(scenario)
            .build();

        ResultAggregator.AggregatedResults results = aggregator.createAggregatedResults();
        results.recordSuccess(100, 200, 1000);
        results.recordSuccess(200, 200, 1000);
        results.recordFailure(50, 500, "Error");

        aggregator.updateExecutionCounts(execution, results);

        assertThat(execution.getExecutedRequests()).isEqualTo(3);
        assertThat(execution.getSuccessfulRequests()).isEqualTo(2);
        assertThat(execution.getFailedRequests()).isEqualTo(1);
    }

    @Test
    void serializeAndDeserializeHistogram_ShouldPreserveData() {
        ResultAggregator.AggregatedResults results = aggregator.createAggregatedResults();

        for (int i = 1; i <= 100; i++) {
            results.recordSuccess(i * 10L, 200, 1000);
        }

        byte[] serialized = aggregator.serializeHistogram(results.getLatencyHistogram());
        Histogram deserialized = aggregator.deserializeHistogram(serialized);

        assertThat(deserialized.getTotalCount()).isEqualTo(100);
        assertThat(deserialized.getMean()).isCloseTo(results.getLatencyHistogram().getMean(), within(1.0));
    }

    private static org.assertj.core.api.NumericAssert within(double tolerance) {
        return new org.assertj.core.api.NumericAssert(tolerance) {};
    }
}