package com.racelab.core.exception;

/**
 * Exception thrown when a test scenario is not found.
 */
public class ScenarioNotFoundException extends RaceLabException {

    public ScenarioNotFoundException(Long id) {
        super("SCENARIO_NOT_FOUND", "Test scenario not found with id: " + id);
    }

    public ScenarioNotFoundException(String name) {
        super("SCENARIO_NOT_FOUND", "Test scenario not found with name: " + name);
    }
}