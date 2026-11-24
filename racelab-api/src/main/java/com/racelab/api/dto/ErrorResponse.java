package com.racelab.api.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response format.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
}