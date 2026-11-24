package com.racelab.core.strategy;

import com.racelab.core.domain.ConcurrencyStrategyType;

import java.util.concurrent.Callable;

/**
 * Interface for concurrency control strategies.
 * Each strategy provides a way to execute operations with specific concurrency guarantees.
 */
public interface ConcurrencyStrategy {

    /**
     * Get the type of this strategy.
     */
    ConcurrencyStrategyType getType();

    /**
     * Execute the given callable with this strategy's concurrency control.
     *
     * @param task the task to execute
     * @param <T>  the result type
     * @return the result of the task
     * @throws Exception if the task throws an exception or the strategy fails
     */
    <T> T execute(Callable<T> task) throws Exception;

    /**
     * Execute the given runnable with this strategy's concurrency control.
     *
     * @param task the task to execute
     * @throws Exception if the task throws an exception or the strategy fails
     */
    void execute(Runnable task) throws Exception;

    /**
     * Get a human-readable description of this strategy.
     */
    String getDescription();

    /**
     * Whether this strategy requires a specific database setup (e.g., version column).
     */
    boolean requiresDatabaseSupport();

    /**
     * Get the expected behavior under high contention.
     */
    ContentionBehavior getContentionBehavior();

    /**
     * Expected behavior when multiple threads contend for the same resource.
     */
    enum ContentionBehavior {
        /** Threads will retry automatically (optimistic locking). */
        RETRY,
        /** Threads will block until lock is acquired (pessimistic locking). */
        BLOCK,
        /** Operations will fail fast with a conflict error. */
        FAIL_FAST,
        /** Operations are serialized by the database (atomic updates). */
        SERIALIZE
    }
}