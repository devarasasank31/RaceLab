package com.racelab.api.dto;

import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.core.domain.HttpMethod;
import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for a test scenario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestScenarioResponse {

    private Long id;
    private String name;
    private String description;
    private Integer concurrentUsers;
    private Integer totalRequests;
    private Integer durationSeconds;
    private Integer rampUpSeconds;
    private String targetEndpoint;
    private HttpMethod httpMethod;
    private Map<String, String> requestHeaders;
    private String requestPayload;
    private ConcurrencyStrategyType concurrencyStrategy;
    private Map<String, Object> strategyConfig;
    private Integer requestTimeoutSeconds;
    private Integer maxRetries;
    private Instant createdAt;
    private Instant updatedAt;
}