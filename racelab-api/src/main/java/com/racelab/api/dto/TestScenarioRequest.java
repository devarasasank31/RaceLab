package com.racelab.api.dto;

import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.core.domain.HttpMethod;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;

/**
 * Request DTO for creating/updating a test scenario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestScenarioRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    @Min(1)
    @Max(10000)
    private Integer concurrentUsers;

    @Min(1)
    @Max(1000000)
    private Integer totalRequests;

    @Min(1)
    @Max(3600)
    private Integer durationSeconds;

    @Min(0)
    @Max(300)
    private Integer rampUpSeconds;

    @NotBlank
    @Size(max = 2048)
    private String targetEndpoint;

    @NotNull
    private HttpMethod httpMethod;

    private Map<String, String> requestHeaders;

    private String requestPayload;

    @Builder.Default
    private ConcurrencyStrategyType concurrencyStrategy = ConcurrencyStrategyType.NONE;

    private Map<String, Object> strategyConfig;

    @Min(1)
    @Max(300)
    @Builder.Default
    private Integer requestTimeoutSeconds = 30;

    @Min(0)
    @Max(10)
    @Builder.Default
    private Integer maxRetries = 0;
}