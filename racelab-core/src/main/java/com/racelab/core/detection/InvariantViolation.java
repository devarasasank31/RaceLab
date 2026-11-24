package com.racelab.core.detection;

import lombok.*;

import java.time.Instant;

/**
 * Represents a violation of a system invariant detected during testing.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class InvariantViolation {

    /** Name of the invariant that was violated. */
    private String invariantName;

    /** Human-readable description of the violation. */
    private String description;

    /** Expected value according to the invariant. */
    private Object expectedValue;

    /** Actual value observed. */
    private Object actualValue;

    /** Severity of the violation. */
    private Severity severity;

    /** Additional context about the violation. */
    private String context;

    /** Timestamp when the violation was detected. */
    @Builder.Default
    private Instant detectedAt = Instant.now();

    /** Type of the invariant violation. */
    private ViolationType type;

    /**
     * Severity levels for invariant violations.
     */
    public enum Severity {
        /** Critical - data corruption or loss detected. */
        CRITICAL,
        /** High - incorrect behavior but no data loss. */
        HIGH,
        /** Medium - potential issue under specific conditions. */
        MEDIUM,
        /** Low - minor deviation from expected behavior. */
        LOW
    }

    /**
     * Categories of invariant violations.
     */
    public enum ViolationType {
        /** Duplicate operations that should have been prevented. */
        DUPLICATE_OPERATION,
        /** Lost updates where one write overwrote another. */
        LOST_UPDATE,
        /** Overselling beyond available inventory. */
        OVERSOLD,
        /** Inconsistent account balances. */
        BALANCE_INCONSISTENCY,
        /** Duplicate event processing. */
        DUPLICATE_EVENT,
        /** Idempotency key collision or failure. */
        IDEMPOTENCY_FAILURE,
        /** Transaction isolation violation. */
        ISOLATION_VIOLATION,
        /** Deadlock or livelock detected. */
        DEADLOCK,
        /** Custom violation type. */
        CUSTOM
    }
}