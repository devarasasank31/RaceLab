package com.racelab.engine.aggregator;

import com.racelab.core.domain.RequestResult;
import com.racelab.core.domain.TestExecution;
import com.racelab.core.domain.TestResult;
import com.racelab.core.util.PercentileCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hdrhistogram.Histogram;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Thread-safe aggregator for test execution results.
 * Uses HdrHistogram for accurate percentile calculations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResultAggregator {

    /**
     * Aggregated results during test execution.
     */
    public static class AggregatedResults {
        private final LongAdder totalRequests = new LongAdder();
        private final LongAdder successfulRequests = new LongAdder();
        private final LongAdder failedRequests = new LongAdder();
        private final LongAdder totalResponseBytes = new LongAdder();
        private final Histogram latencyHistogram;
        private final Map<Integer, LongAdder> statusCodeCounts = new ConcurrentHashMap<>();
        private final Map<String, LongAdder> errorCounts = new ConcurrentHashMap<>();
        private final Instant startTime;
        private volatile Instant endTime;

        public AggregatedResults(Histogram histogram) {
            this.latencyHistogram = histogram;
            this.startTime = Instant.now();
        }

        public void recordSuccess(long latencyMs, int statusCode, long responseBytes) {
            totalRequests.increment();
            successfulRequests.increment();
            totalResponseBytes.add(responseBytes);
            latencyHistogram.recordValue(latencyMs * 1000); // Convert to microseconds
            statusCodeCounts.computeIfAbsent(statusCode, k -> new LongAdder()).increment();
        }

        public void recordFailure(long latencyMs, int statusCode, String errorMessage) {
            totalRequests.increment();
            failedRequests.increment();
            if (latencyMs > 0) {
                latencyHistogram.recordValue(latencyMs * 1000);
            }
            if (statusCode > 0) {
                statusCodeCounts.computeIfAbsent(statusCode, k -> new LongAdder()).increment();
            }
            if (errorMessage != null) {
                errorCounts.computeIfAbsent(errorMessage, k -> new LongAdder()).increment();
            }
        }

        public void markEnd() {
            this.endTime = Instant.now();
        }

        public long getTotalRequests() { return totalRequests.sum(); }
        public long getSuccessfulRequests() { return successfulRequests.sum(); }
        public long getFailedRequests() { return failedRequests.sum(); }
        public long getTotalResponseBytes() { return totalResponseBytes.sum(); }
        public Histogram getLatencyHistogram() { return latencyHistogram; }
        public Map<Integer, Long> getStatusCodeDistribution() {
            return statusCodeCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum()));
        }
        public Map<String, Long> getErrorBreakdown() {
            return errorCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum()));
        }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
    }

    /**
     * Create a new aggregated results container.
     */
    public AggregatedResults createAggregatedResults() {
        Histogram histogram = new Histogram(
            1, // 1 microsecond
            3_600_000_000L, // 1 hour in microseconds
            3 // significant figures
        );
        return new AggregatedResults(histogram);
    }

    /**
     * Build a TestResult entity from aggregated results.
     */
    public TestResult buildTestResult(TestExecution execution, AggregatedResults results) {
        results.markEnd();

        PercentileCalculator.Percentiles percentiles = PercentileCalculator.getPercentiles(results.getLatencyHistogram());

        long durationMs = java.time.Duration.between(results.getStartTime(), results.getEndTime()).toMillis();
        double requestsPerSecond = durationMs > 0
            ? (results.getTotalRequests() * 1000.0) / durationMs
            : 0.0;

        double throughputMbps = durationMs > 0
            ? (results.getTotalResponseBytes() * 8.0) / (durationMs * 1000.0) // bits per second -> Mbps
            : 0.0;

        double errorRate = results.getTotalRequests() > 0
            ? (results.getFailedRequests() * 100.0) / results.getTotalRequests()
            : 0.0;

        // Serialize histogram for storage
        byte[] histogramBytes = serializeHistogram(results.getLatencyHistogram());

        return TestResult.builder()
            .execution(execution)
            .totalRequests(results.getTotalRequests())
            .successfulRequests(results.getSuccessfulRequests())
            .failedRequests(results.getFailedRequests())
            .requestsPerSecond(requestsPerSecond)
            .avgLatencyMs(percentiles.meanMs())
            .medianLatencyMs(percentiles.medianMs())
            .p95LatencyMs(percentiles.p95Ms())
            .p99LatencyMs(percentiles.p99Ms())
            .minLatencyMs(percentiles.minMs())
            .maxLatencyMs(percentiles.maxMs())
            .stdDeviationMs(percentiles.stdDevMs())
            .errorRatePercent(errorRate)
            .throughputMbps(throughputMbps)
            .latencyHistogram(histogramBytes)
            .errorBreakdown(results.getErrorBreakdown())
            .statusCodeDistribution(results.getStatusCodeDistribution())
            .build();
    }

    /**
     * Update TestExecution with aggregated counts.
     */
    public void updateExecutionCounts(TestExecution execution, AggregatedResults results) {
        execution.setExecutedRequests(results.getTotalRequests());
        execution.setSuccessfulRequests(results.getSuccessfulRequests());
        execution.setFailedRequests(results.getFailedRequests());
    }

    /**
     * Serialize histogram to byte array for storage.
     */
    private byte[] serializeHistogram(Histogram histogram) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            histogram.outputPercentileDistribution(dos, 1000.0); // Output in milliseconds
            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Failed to serialize histogram", e);
            return new byte[0];
        }
    }

    /**
     * Deserialize histogram from byte array.
     */
    public Histogram deserializeHistogram(byte[] data) {
        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
            java.io.DataInputStream dis = new java.io.DataInputStream(bais);
            return Histogram.decodeFromCompressedDataStream(dis);
        } catch (Exception e) {
            log.warn("Failed to deserialize histogram", e);
            return new Histogram(1, 3_600_000_000L, 3);
        }
    }
}