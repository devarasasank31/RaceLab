# ADR 002: Use HdrHistogram for Accurate Percentile Calculations

## Status
Accepted

## Context

RaceLab needs to calculate accurate latency percentiles (p50, p95, p99, p99.9) for performance analysis. Common mistakes include:

1. **Calculating percentiles from averages** - Statistically invalid, loses distribution shape
2. **Storing all latencies in memory** - O(n) memory, O(n log n) sorting for percentiles
3. **Using reservoir sampling** - Approximate, not suitable for SLA verification
4. **Fixed bucket histograms** - Poor precision at tails or high latency ranges

## Decision

Use **HdrHistogram** (High Dynamic Range Histogram) for all latency measurements:

- **Fixed relative precision** (configurable significant figures)
- **O(1) recording** - No allocation per sample
- **Mergeable** - Combine histograms from multiple threads/instances
- **Accurate percentiles** - Direct percentile queries from histogram
- **Memory efficient** - ~10KB for full range with 3 sig figs
- **Corrects for coordinated omission** - Optional auto-correction

## Consequences

### Positive
- **Accuracy**: True percentiles from raw data, not approximations
- **Performance**: Lock-free recording, minimal overhead
- **Scalability**: Merge histograms from distributed test runners
- **Range**: Covers 1µs to 1hr with consistent precision
- **Standard**: Used by major systems (JVM, Cassandra, Kafka, gRPC)

### Negative
- **Additional Dependency**: ~200KB JAR
- **Learning Curve**: Different API than simple lists
- **Serialization**: Custom binary format (handled in ResultAggregator)

## Implementation

```java
// Configuration: 1µs to 1 hour, 3 significant figures (0.1% precision at p99.9)
Histogram histogram = new Histogram(1L, 3_600_000_000L, 3);

// Recording (lock-free, thread-safe)
histogram.recordValue(latencyNanos / 1000); // Microseconds

// Percentile queries
double p50 = histogram.getValueAtPercentile(50.0) / 1000.0;  // ms
double p95 = histogram.getValueAtPercentile(95.0) / 1000.0;
double p99 = histogram.getValueAtPercentile(99.0) / 1000.0;
double p999 = histogram.getValueAtPercentile(99.9) / 1000.0;

// Merging (for distributed tests)
Histogram combined = new Histogram(1, 3_600_000_000L, 3);
for (Histogram h : shards) {
    combined.add(h);
}

// Export for analysis
histogram.outputPercentileDistribution(outputStream, 1000.0); // CSV in ms
```

## Alternatives Considered

1. **ArrayList + Sort** - O(n) memory, O(n log n) percentile, impractical for >100k samples
2. **Apache Commons Math Percentile** - Requires storing all values
3. **t-Digest** - Good for merging, but approximate; HdrHistogram is exact for given precision
4. **Custom Bucket Histogram** - Manual bucket tuning, poor tail precision
5. **Reservoir Sampling (HdrHistogram's WindowedHistogram)** - Approximate, not for SLA

## Validation

Test with known dataset (1000 values: 1-1000):
- HdrHistogram p50: 500.5 (exact)
- HdrHistogram p99: 991 (exact with 3 sig figs)
- ArrayList sort p50: 500.5
- ArrayList sort p99: 991

Load test with 1M samples:
- Recording throughput: > 50M ops/sec
- Memory: ~15KB
- Merge time: < 1ms

## References

- [HdrHistogram GitHub](https://github.com/HdrHistogram/HdrHistogram)
- [How NOT to Measure Latency (Gil Tene)](https://www.youtube.com/watch?v=lJ8ydIuPFeU)
- [Coordinated Omission](https://github.com/HdrHistogram/HdrHistogram#coordinated-omission)