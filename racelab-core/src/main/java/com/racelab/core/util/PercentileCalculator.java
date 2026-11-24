package com.racelab.core.util;

import org.hdrhistogram.Histogram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongConsumer;

/**
 * Utility for calculating accurate percentiles using HdrHistogram.
 * Avoids the common mistake of calculating percentiles from averages.
 */
public final class PercentileCalculator {

    private static final long LOWEST_TRACKABLE_VALUE = 1L; // 1 microsecond
    private static final long HIGHEST_TRACKABLE_VALUE = 3_600_000_000L; // 1 hour in microseconds
    private static final int SIGNIFICANT_FIGURES = 3;

    private PercentileCalculator() {
        // Utility class
    }

    /**
     * Create a new histogram for recording latencies.
     */
    public static Histogram createHistogram() {
        return new Histogram(LOWEST_TRACKABLE_VALUE, HIGHEST_TRACKABLE_VALUE, SIGNIFICANT_FIGURES);
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
        // This is a placeholder - in practice you'd use histogram.recordValueWithCount
        // or record directly in the consumer
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

    /**
     * Calculate percentiles from a list of latencies (for verification/testing).
     * This uses sorting and is O(n log n) - only for small datasets or verification.
     */
    public static Percentiles calculateFromList(List<Long> latenciesMs) {
        if (latenciesMs.isEmpty()) {
            return new Percentiles(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        List<Long> sorted = new ArrayList<>(latenciesMs);
        Collections.sort(sorted);

        long count = sorted.size();
        double sum = sorted.stream().mapToLong(Long::longValue).sum();
        double mean = sum / count;

        double min = sorted.get(0);
        double max = sorted.get(count - 1);

        double median = percentile(sorted, 50);
        double p95 = percentile(sorted, 95);
        double p99 = percentile(sorted, 99);
        double p999 = percentile(sorted, 99.9);

        // Standard deviation
        double variance = sorted.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .sum() / count;
        double stdDev = Math.sqrt(variance);

        return new Percentiles(mean, median, p95, p99, p999, min, max, stdDev, count);
    }

    private static double percentile(List<Long> sorted, double p) {
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }
}