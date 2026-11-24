package com.racelab.core.exception;

/**
 * Exception thrown for validation errors.
 */
public class ValidationException extends RaceLabException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}