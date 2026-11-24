package com.racelab.core.exception;

/**
 * Exception thrown when a test execution is not found.
 */
public class ExecutionNotFoundException extends RaceLabException {

    public ExecutionNotFoundException(Long id) {
        super("EXECUTION_NOT_FOUND", "Test execution not found with id: " + id);
    }
}