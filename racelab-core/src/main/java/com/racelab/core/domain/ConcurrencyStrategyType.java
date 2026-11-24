package com.racelab.core.domain;

/**
 * Types of concurrency control strategies that can be tested.
 */
public enum ConcurrencyStrategyType {
    /** No explicit concurrency protection - baseline for comparison. */
    NONE("No Protection", "Baseline with no concurrency control", false),

    /** Optimistic locking using JPA @Version annotation. */
    OPTIMISTIC_LOCKING("Optimistic Locking", "JPA @Version with retry on conflict", false),

    /** Pessimistic locking using SELECT FOR UPDATE. */
    PESSIMISTIC_LOCKING("Pessimistic Locking", "Database row-level locks", false),

    /** Atomic database updates using conditional UPDATE statements. */
    ATOMIC_UPDATE("Atomic Update", "Single UPDATE with WHERE clause guard", false),

    /** Idempotency key based deduplication. */
    IDEMPOTENCY_KEY("Idempotency Key", "Client-provided keys for deduplication", true),

    /** Redis-based distributed locking. */
    REDIS_DISTRIBUTED_LOCK("Redis Distributed Lock", "Redlock algorithm for distributed coordination", false);

    private final String displayName;
    private final String description;
    private final boolean requiresIdempotencyKey;

    ConcurrencyStrategyType(String displayName, String description, boolean requiresIdempotencyKey) {
        this.displayName = displayName;
        this.description = description;
        this.requiresIdempotencyKey = requiresIdempotencyKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresIdempotencyKey() {
        return requiresIdempotencyKey;
    }
}