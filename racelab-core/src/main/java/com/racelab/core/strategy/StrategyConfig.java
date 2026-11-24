package com.racelab.core.strategy;

import lombok.*;

import java.util.Map;

/**
 * Configuration for a concurrency strategy.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class StrategyConfig {

    private ConcurrencyStrategyType type;

    /** Maximum retry attempts for retry-based strategies. */
    @Builder.Default
    private int maxRetries = 3;

    /** Base delay between retries in milliseconds. */
    @Builder.Default
    private long retryDelayMs = 10;

    /** Maximum delay between retries in milliseconds. */
    @Builder.Default
    private long maxRetryDelayMs = 1000;

    /** Exponential backoff multiplier. */
    @Builder.Default
    private double backoffMultiplier = 2.0;

    /** Lock timeout in milliseconds for blocking strategies. */
    @Builder.Default
    private long lockTimeoutMs = 5000;

    /** Whether to use virtual threads for I/O-bound work. */
    @Builder.Default
    private boolean useVirtualThreads = true;

    /** Additional strategy-specific configuration. */
    private Map<String, Object> customConfig;
}