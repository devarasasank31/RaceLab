package com.racelab.api.dto;

import com.racelab.core.domain.TestStatus;
import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for a test execution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecutionResponse {

    private Long id;
    private Long scenarioId;
    private String scenarioName;
    private TestStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private String errorMessage;
    private Long executedRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private Map<String, Object> configurationSnapshot;
    private Instant createdAt;
    private Instant updatedAt;

    public double getSuccessRate() {
        if (executedRequests == null || executedRequests == 0) return 0.0;
        return (double) successfulRequests / executedRequests * 100.0;
    }

    public double getErrorRate() {
        if (executedRequests == null || executedRequests == 0) return 0.0;
        return (double) failedRequests / executedRequests * 100.0;
    }

    public long getDurationMillis() {
        if (startedAt == null) return 0;
        Instant end = completedAt != null ? completedAt : cancelledAt != null ? cancelledAt : Instant.now();
        return java.time.Duration.between(startedAt, end).toMillis();
    }

    public boolean isTerminal() {
        return status == TestStatus.COMPLETED
            || status == TestStatus.CANCELLED
            || status == TestStatus.FAILED;
    }
}