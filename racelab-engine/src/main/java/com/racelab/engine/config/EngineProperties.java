package com.racelab.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the concurrency test engine.
 */
@Data
@Component
@ConfigurationProperties(prefix = "racelab.engine")
public class EngineProperties {

    /** Default number of concurrent users if not specified in scenario. */
    private int defaultConcurrentUsers = 10;

    /** Default request timeout in seconds. */
    private int defaultRequestTimeoutSeconds = 30;

    /** Maximum allowed concurrent users. */
    private int maxConcurrentUsers = 10000;

    /** Maximum total requests per test. */
    private long maxTotalRequests = 1_000_000;

    /** Maximum test duration in seconds. */
    private int maxDurationSeconds = 3600;

    /** Default ramp-up period in seconds. */
    private int defaultRampUpSeconds = 0;

    /** Thread pool core size for CPU-bound work. */
    private int cpuThreadPoolCoreSize = 4;

    /** Thread pool max size for CPU-bound work. */
    private int cpuThreadPoolMaxSize = 16;

    /** Thread pool queue capacity. */
    private int threadPoolQueueCapacity = 10000;

    /** Whether to use virtual threads for I/O-bound HTTP requests. */
    private boolean useVirtualThreads = true;

    /** Maximum time to wait for graceful shutdown in seconds. */
    private int shutdownTimeoutSeconds = 30;

    /** Enable detailed per-request logging (performance impact). */
    private boolean detailedLogging = false;

    /** HdrHistogram configuration. */
    private Histogram histogram = new Histogram();

    @Data
    public static class Histogram {
        /** Lowest trackable value in microseconds. */
        private long lowestTrackableValue = 1;

        /** Highest trackable value in microseconds (1 hour). */
        private long highestTrackableValue = 3_600_000_000L;

        /** Number of significant figures. */
        private int significantFigures = 3;
    }
}