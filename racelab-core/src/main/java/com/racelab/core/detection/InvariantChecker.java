package com.racelab.core.detection;

import com.racelab.core.domain.TestExecution;

import java.util.List;

/**
 * Interface for checking invariants after test execution.
 * Implementations verify that system invariants hold under concurrent access.
 */
public interface InvariantChecker {

    /**
     * Get the name of the invariant being checked.
     */
    String getInvariantName();

    /**
     * Get a description of what this invariant ensures.
     */
    String getDescription();

    /**
     * Check the invariant against the test execution results.
     *
     * @param execution the completed test execution
     * @return list of violations found (empty if no violations)
     */
    List<InvariantViolation> check(TestExecution execution);

    /**
     * Whether this checker requires access to the database for verification.
     */
    boolean requiresDatabaseAccess();
}