package com.racelab.engine.dto;

import com.racelab.core.domain.ConcurrencyStrategyType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * Request DTO for running race condition demonstrations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceConditionDemoRequest {

    @NotBlank
    @Size(max = 100)
    private String eventId;

    @Min(1)
    @Max(1000)
    @Builder.Default
    private Integer totalTickets = 1;

    @Min(1)
    @Max(10000)
    @Builder.Default
    private Integer concurrentRequests = 1000;

    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer quantityPerRequest = 1;

    private List<ConcurrencyStrategyType> strategies;

    private boolean runVulnerable = true;
}