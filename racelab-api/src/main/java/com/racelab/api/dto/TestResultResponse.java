package com.racelab.api.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for test results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResultResponse {

    private Long id;
    private Long executionId;
    private Long totalRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private Double requestsPerSecond;
    private Double avgLatencyMs;
    private Double medianLatencyMs;
    private Double p95LatencyMs;
    private Double p99LatencyMs;
    private Double minLatencyMs;
    private Double maxLatencyMs;
    private Double stdDeviationMs;
    private Double errorRatePercent;
    private Double throughputMbps;
    private Map<String, Long> errorBreakdown;
    private Map<Integer, Long> statusCodeDistribution;
    private Instant createdAt;
}