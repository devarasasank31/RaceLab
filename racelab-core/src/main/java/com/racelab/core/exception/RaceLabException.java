package com.racelab.core.exception;

/**
 * Base exception for RaceLab domain errors.
 * Unchecked exception for cleaner service layer code.
 */
public class RaceLabException extends RuntimeException {

    private final String errorCode;

    public RaceLabException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RaceLabException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}