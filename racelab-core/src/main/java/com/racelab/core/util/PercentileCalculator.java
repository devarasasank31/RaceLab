package com.racelab.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongConsumer;

/**
 * Utility for calculating accurate percentiles using a sorted list.
 * Avoids the common mistake of calculating percentiles from averages.
 * Thread-safe implementation using synchronized list.
 */
public final class PercentileCalculator {

    /**
     * A thread-safe histogram implementation using a synchronized list.
     * Stores all recorded values for accurate percentile calculation.
     */
    public static final class Histogram {
        private final List<Long> values = Collections.synchronizedList(new ArrayList<>());

        public void recordValue(long value) {
            values.add(value);
        }

        public void add(Histogram other) {
            values.addAll(other.values);
        }

        public long getTotalCount() {
            synchronized (values) {
                return values.size();
            }
        }

        public double getMean() {
            synchronized (values) {
                if (values.isEmpty()) return 0.0;
                return values.stream().mapToLong(Long::longValue).average().orElse(0.0);
            }
        }

        public long getMinValue() {
            synchronized (values) {
                if (values.isEmpty()) return 0L;
                return Collections.min(values);
            }
        }

        public long getMaxValue() {
            synchronized (values) {
                if (values.isEmpty()) return 0L;
                return Collections.max(values);
            }
        }

        public double getStdDeviation() {
            synchronized (values) {
                if (values.isEmpty()) return 0.0;
                double mean = getMean();
                double variance = values.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2))
                    .average()
                    .orElse(0.0);
                return Math.sqrt(variance);
            }
        }

        public long getValueAtPercentile(double percentile) {
            synchronized (values) {
                if (values.isEmpty()) return 0L;
                List<Long> sorted = new ArrayList<>(values);
                Collections.sort(sorted);
                int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
                index = Math.max(0, Math.min(index, sorted.size() - 1));
                return sorted.get(index);
            }
        }

        public List<Long> getValues() {
            synchronized (values) {
                return new ArrayList<>(values);
            }
        }
    }

    private PercentileCalculator() {
        // Utility class
    }

    /**
     * Create a new histogram for recording latencies.
     */
    public static Histogram createHistogram() {
        return new Histogram();
    }

    /**
     * Record a latency value in microseconds.
     */
    public static void record(Histogram histogram, long latencyMicros) {
        histogram.recordValue(latencyMicros);
    }

    /**
     * Record a latency value in milliseconds.
     */
    public static void recordMs(Histogram histogram, long latencyMs) {
        histogram.recordValue(latencyMs * 1000); // Convert to microseconds
    }

    /**
     * Get the value at a given percentile (in microseconds).
     */
    public static long getPercentile(Histogram histogram, double percentile) {
        return histogram.getValueAtPercentile(percentile);
    }

    /**
     * Get the value at a given percentile (in milliseconds).
     */
    public static double getPercentileMs(Histogram histogram, double percentile) {
        return histogram.getValueAtPercentile(percentile) / 1000.0;
    }

    /**
     * Get common percentiles as a record.
     */
    public static Percentiles getPercentiles(Histogram histogram) {
        return new Percentiles(
            histogram.getMean() / 1000.0,
            getPercentileMs(histogram, 50.0),
            getPercentileMs(histogram, 95.0),
            getPercentileMs(histogram, 99.0),
            getPercentileMs(histogram, 99.9),
            histogram.getMinValue() / 1000.0,
            histogram.getMaxValue() / 1000.0,
            histogram.getStdDeviation() / 1000.0,
            histogram.getTotalCount()
        );
    }

    /**
     * Record multiple values from a list of latencies in milliseconds.
     */
    public static void recordAll(Histogram histogram, List<Long> latenciesMs) {
        for (long latency : latenciesMs) {
            recordMs(histogram, latency);
        }
    }

    /**
     * Record values from a LongConsumer (e.g., parallel stream).
     */
    public static void recordFrom(Histogram histogram, LongConsumer consumer) {
        // Placeholder for future implementation
    }

    /**
     * Merge another histogram into this one.
     */
    public static void merge(Histogram target, Histogram source) {
        target.add(source);
    }

    /**
     * Percentiles result record.
     */
    public record Percentiles(
        double meanMs,
        double medianMs,
        double p95Ms,
        double p99Ms,
        double p999Ms,
        double minMs,
        double maxMs,
        double stdDevMs,
        long count
    ) {
        public boolean hasData() {
            return count > 0;
        }
    }
}