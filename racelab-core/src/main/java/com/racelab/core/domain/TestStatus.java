package com.racelab.core.domain;

/**
 * Status of a test execution.
 */
public enum TestStatus {
    /** Test has been created but not started. */
    PENDING,
    /** Test is currently running. */
    RUNNING,
    /** Test completed successfully. */
    COMPLETED,
    /** Test was cancelled by user. */
    CANCELLED,
    /** Test failed due to an error. */
    FAILED,
    /** Test completed but with invariant violations detected. */
    COMPLETED_WITH_VIOLATIONS
}