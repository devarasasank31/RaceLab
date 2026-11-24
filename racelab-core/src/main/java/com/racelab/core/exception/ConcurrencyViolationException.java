package com.racelab.core.exception;

/**
 * Exception thrown when a concurrency invariant is violated.
 */
public class ConcurrencyViolationException extends RaceLabException {

    private final String invariantName;
    private final Object expectedValue;
    private final Object actualValue;

    public ConcurrencyViolationException(String invariantName, Object expectedValue, Object actualValue) {
        super("CONCURRENCY_VIOLATION",
            String.format("Invariant '%s' violated: expected %s, actual %s", invariantName, expectedValue, actualValue));
        this.invariantName = invariantName;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    public String getInvariantName() {
        return invariantName;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    public Object getActualValue() {
        return actualValue;
    }
}