package com.racelab.core.exception;

/**
 * Exception thrown when a test execution fails due to configuration or runtime errors.
 */
public class TestExecutionException extends RaceLabException {

    public TestExecutionException(String message) {
        super("TEST_EXECUTION_ERROR", message);
    }

    public TestExecutionException(String message, Throwable cause) {
        super("TEST_EXECUTION_ERROR", message, cause);
    }
}