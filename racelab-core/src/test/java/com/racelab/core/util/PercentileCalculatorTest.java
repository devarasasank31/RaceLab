package com.racelab.core.util;

import org.hdrhistogram.Histogram;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PercentileCalculator using HdrHistogram.
 * Verifies accurate percentile calculations against known datasets.
 */
class PercentileCalculatorTest {

    @Test
    void createHistogram_ShouldCreateValidHistogram() {
        Histogram histogram = PercentileCalculator.createHistogram();

        assertThat(histogram).isNotNull();
        assertThat(histogram.getTotalCount()).isZero();
    }

    @Test
    void recordAndGetPercentile_SingleValue_ShouldReturnSameValue() {
        Histogram histogram = PercentileCalculator.createHistogram();
        long latencyMs = 100L;

        PercentileCalculator.recordMs(histogram, latencyMs);

        assertThat(histogram.getTotalCount()).isEqualTo(1);
        assertThat(PercentileCalculator.getPercentileMs(histogram, 50.0)).isEqualTo(100.0);
        assertThat(PercentileCalculator.getPercentileMs(histogram, 99.0)).isEqualTo(100.0);
    }

    @Test
    void getPercentiles_KnownDataset_ShouldMatchSortedCalculation() {
        // Known dataset: 1, 2, 3, ..., 1000
        List<Long> latencies = new ArrayList<>();
        for (long i = 1; i <= 1000; i++) {
            latencies.add(i);
        }

        Histogram histogram = PercentileCalculator.createHistogram();
        PercentileCalculator.recordAll(histogram, latencies);

        PercentileCalculator.Percentiles hdrPercentiles = PercentileCalculator.getPercentiles(histogram);
        PercentileCalculator.Percentiles sortedPercentiles = PercentileCalculator.calculateFromList(latencies);

        // HdrHistogram with 3 significant figures should be very close
        assertThat(hdrPercentiles.meanMs()).isCloseTo(sortedPercentiles.meanMs(), within(0.1));
        assertThat(hdrPercentiles.medianMs()).isCloseTo(sortedPercentiles.medianMs(), within(1.0));
        assertThat(hdrPercentiles.p95Ms()).isCloseTo(sortedPercentiles.p95Ms(), within(1.0));
        assertThat(hdrPercentiles.p99Ms()).isCloseTo(sortedPercentiles.p99Ms(), within(1.0));
        assertThat(hdrPercentiles.minMs()).isEqualTo(sortedPercentiles.minMs());
        assertThat(hdrPercentiles.maxMs()).isEqualTo(sortedPercentiles.maxMs());
    }

    @Test
    void getPercentiles_LargeDataset_ShouldBeAccurate() {
        // 100,000 values from 1-10000
        List<Long> latencies = new ArrayList<>(100_000);
        for (int i = 0; i < 100_000; i++) {
            latencies.add((long) (ThreadLocalRandom.current().nextDouble() * 10000) + 1);
        }

        Histogram histogram = PercentileCalculator.createHistogram();
        PercentileCalculator.recordAll(histogram, latencies);

        PercentileCalculator.Percentiles hdrPercentiles = PercentileCalculator.getPercentiles(histogram);
        PercentileCalculator.Percentiles sortedPercentiles = PercentileCalculator.calculateFromList(latencies);

        // With 3 sig figs, should be within 0.5% for percentiles
        double tolerance = 0.005; // 0.5%
        assertThat(hdrPercentiles.p50Ms()).isCloseTo(sortedPercentiles.medianMs(), within(sortedPercentiles.medianMs() * tolerance));
        assertThat(hdrPercentiles.p95Ms()).isCloseTo(sortedPercentiles.p95Ms(), within(sortedPercentiles.p95Ms() * tolerance));
        assertThat(hdrPercentiles.p99Ms()).isCloseTo(sortedPercentiles.p99Ms(), within(sortedPercentiles.p99Ms() * tolerance));
    }

    @Test
    void mergeHistograms_ShouldCombineCorrectly() {
        Histogram h1 = PercentileCalculator.createHistogram();
        Histogram h2 = PercentileCalculator.createHistogram();

        PercentileCalculator.recordMs(h1, 100);
        PercentileCalculator.recordMs(h1, 200);
        PercentileCalculator.recordMs(h2, 300);
        PercentileCalculator.recordMs(h2, 400);

        PercentileCalculator.merge(h1, h2);

        assertThat(h1.getTotalCount()).isEqualTo(4);
        assertThat(PercentileCalculator.getPercentileMs(h1, 50.0)).isEqualTo(250.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {50.0, 95.0, 99.0, 99.9})
    void getPercentile_EdgeCases_ShouldNotThrow(double percentile) {
        Histogram histogram = PercentileCalculator.createHistogram();
        PercentileCalculator.recordMs(histogram, 1);
        PercentileCalculator.recordMs(histogram, 1000);

        double result = PercentileCalculator.getPercentileMs(histogram, percentile);

        assertThat(result).isBetween(1.0, 1000.0);
    }

    @Test
    void calculateFromList_EmptyList_ShouldReturnZeros() {
        PercentileCalculator.Percentiles result = PercentileCalculator.calculateFromList(new ArrayList<>());

        assertThat(result.count()).isZero();
        assertThat(result.meanMs()).isZero();
        assertThat(result.medianMs()).isZero();
    }

    @Test
    void calculateFromList_SingleElement_ShouldReturnThatElement() {
        List<Long> list = List.of(42L);
        PercentileCalculator.Percentiles result = PercentileCalculator.calculateFromList(list);

        assertThat(result.count()).isEqualTo(1);
        assertThat(result.meanMs()).isEqualTo(42.0);
        assertThat(result.medianMs()).isEqualTo(42.0);
        assertThat(result.p95Ms()).isEqualTo(42.0);
        assertThat(result.p99Ms()).isEqualTo(42.0);
        assertThat(result.minMs()).isEqualTo(42.0);
        assertThat(result.maxMs()).isEqualTo(42.0);
    }

    @Test
    void recordValue_Microseconds_ShouldConvertCorrectly() {
        Histogram histogram = PercentileCalculator.createHistogram();
        long latencyMicros = 5000; // 5ms in microseconds

        PercentileCalculator.record(histogram, latencyMicros);

        assertThat(PercentileCalculator.getPercentileMs(histogram, 50.0)).isEqualTo(5.0);
    }

    @RepeatedTest(10)
    void concurrentRecording_ShouldBeThreadSafe() throws InterruptedException {
        Histogram histogram = PercentileCalculator.createHistogram();
        int threadCount = 100;
        int recordsPerThread = 1000;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < recordsPerThread; j++) {
                    PercentileCalculator.recordMs(histogram, ThreadLocalRandom.current().nextLong(1, 1000));
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertThat(histogram.getTotalCount()).isEqualTo((long) threadCount * recordsPerThread);
    }

    private static org.assertj.core.api.NumericAssert within(double tolerance) {
        return new org.assertj.core.api.NumericAssert(tolerance) {};
    }
}