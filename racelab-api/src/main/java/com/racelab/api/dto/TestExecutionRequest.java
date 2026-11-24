package com.racelab.api.dto;

import lombok.*;

/**
 * Request DTO for starting a test execution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecutionRequest {

    /** Optional override for concurrent users. */
    private Integer concurrentUsers;

    /** Optional override for total requests. */
    private Integer totalRequests;

    /** Optional override for duration seconds. */
    private Integer durationSeconds;

    /** Optional override for ramp-up seconds. */
    private Integer rampUpSeconds;
}