# Benchmarking Methodology

This document describes the benchmarking methodology used in RaceLab for accurate, reproducible performance measurements.

## Principles

1. **Measure, don't guess** - Use accurate instrumentation
2. **Percentiles from raw data** - Never calculate percentiles from averages
3. **Warm-up runs** - Allow JIT compilation and cache warming
4. **Steady-state measurement** - Measure after warm-up, not during ramp-up
5. **Controlled environment** - Isolated infrastructure, no noisy neighbors
6. **Statistical rigor** - Sufficient sample sizes, confidence intervals

## Metrics Collected

### Latency Metrics (per request)
- **Min/Max** - Range
- **Mean** - Average (use with caution)
- **Median (p50)** - Typical experience
- **p95** - SLA target for most requests
- **p99** - Tail latency
- **p99.9** - Extreme tail
- **Standard Deviation** - Variability

### Throughput Metrics
- **Requests/second** - Overall throughput
- **Successful requests/second** - Goodput
- **Error rate** - Failed requests / total

### Resource Metrics
- **CPU utilization** - Per process and system
- **Memory usage** - Heap, off-heap, RSS
- **GC pauses** - Young/Old generation
- **Thread count** - Platform vs virtual
- **Database connections** - Active/idle
- **Network I/O** - Bytes sent/received

## HdrHistogram for Accurate Percentiles

### Why Not Average-Based Percentiles?

```java
// WRONG - Calculating percentiles from averages
List<Double> averages = runMultipleTests();
double p99 = percentile(averages, 99); // Statistically invalid!

// CORRECT - Record every request latency
Histogram histogram = new Histogram(1, 3_600_000_000L, 3); // 1µs to 1hr, 3 sig figs
for (eachRequest) {
    histogram.recordValue(latencyMicros);
}
double p99 = histogram.getValueAtPercentile(99.0); // Accurate!
```

### HdrHistogram Configuration

```java
// Latency range: 1 microsecond to 1 hour
// Precision: 3 significant figures (0.1% accuracy at 99.9th percentile)
Histogram histogram = new Histogram(
    1L,                           // lowestTrackableValue (1 µs)
    3_600_000_000L,              // highestTrackableValue (1 hour in µs)
    3                             // numberOfSignificantValueDigits
);

// Recording
histogram.recordValue(latencyNanos / 1000); // Convert to microseconds

// Merging (for distributed tests)
Histogram combined = new Histogram(1, 3_600_000_000L, 3);
for (Histogram h : histograms) {
    combined.add(h);
}
```

### Percentile Output

```java
// Key percentiles for SLA monitoring
double p50 = histogram.getValueAtPercentile(50.0) / 1000.0;  // ms
double p95 = histogram.getValueAtPercentile(95.0) / 1000.0;
double p99 = histogram.getValueAtPercentile(99.0) / 1000.0;
double p999 = histogram.getValueAtPercentile(99.9) / 1000.0;

// Export for analysis
histogram.outputPercentileDistribution(System.out, 1000.0); // CSV in ms
```

## Test Execution Phases

### 1. Warm-up Phase (discard results)
```java
// 30-60 seconds or 10% of total requests
// Allows:
// - JIT compilation (C2)
// - Class loading
// - Connection pool warming
// - Database plan caching
// - OS page cache warming
```

### 2. Ramp-up Phase (gradual load increase)
```java
// Linear or exponential ramp
// Prevents thundering herd
// Allows auto-scaling (if applicable)
// Typically 10-30% of test duration
```

### 3. Measurement Phase (steady state)
```java
// Main measurement window
// Minimum 30 seconds for statistical significance
// Longer for low-throughput tests
// Capture all metrics here
```

### 4. Cool-down Phase
```java
// Allow in-flight requests to complete
// Graceful shutdown
// Final metric collection
```

## Statistical Requirements

### Sample Size
- **Minimum**: 10,000 requests for percentile accuracy
- **Recommended**: 100,000+ for tail percentiles (p99.9)
- **Duration**: At least 30 seconds steady state

### Confidence Intervals
```java
// For p99 with 100k samples: ~±0.5% relative error
// For p99.9 with 100k samples: ~±5% relative error
// Need 1M+ samples for tight p99.9 confidence
```

### Outlier Handling
- **Do not discard outliers** - They are real tail latency events
- **Record max** - Shows worst-case
- **Use HdrHistogram** - Handles full range without clamping

## Benchmark Scenarios

### Scenario 1: Baseline Throughput
```
Target: Local service (no network)
Concurrency: 100 virtual threads
Duration: 60s measurement
Expected: >100k req/s for simple endpoint
```

### Scenario 2: Database Contention
```
Target: Ticket booking (1 row)
Concurrency: 1000 virtual threads
Duration: 30s measurement
Strategies: Compare all 6 concurrency strategies
Metrics: Success rate, latency, retries
```

### Scenario 3: Network Latency
```
Target: Remote service (1ms RTT)
Concurrency: 500 virtual threads
Duration: 60s measurement
Metrics: Latency distribution, throughput
```

### Scenario 4: Failure Injection
```
Target: Service with 10% error injection
Concurrency: 100 virtual threads
Duration: 60s measurement
Metrics: Error rate, retry behavior, recovery time
```

## Reporting Format

### JSON Results
```json
{
  "testId": "uuid",
  "scenario": "ticket-booking-optimistic",
  "startTime": "2026-01-15T10:00:00Z",
  "endTime": "2026-01-15T10:01:30Z",
  "configuration": {
    "concurrentUsers": 1000,
    "durationSeconds": 60,
    "rampUpSeconds": 10,
    "targetEndpoint": "http://localhost:8080/api/book",
    "strategy": "OPTIMISTIC_LOCKING"
  },
  "results": {
    "totalRequests": 58432,
    "successfulRequests": 58431,
    "failedRequests": 1,
    "requestsPerSecond": 973.8,
    "latencyMs": {
      "min": 1.2,
      "max": 245.6,
      "mean": 12.4,
      "stdDev": 8.7,
      "p50": 10.1,
      "p95": 28.3,
      "p99": 45.2,
      "p999": 89.1
    },
    "errorRatePercent": 0.0017,
    "throughputMbps": 0.45,
    "statusCodeDistribution": {
      "200": 58431,
      "409": 1
    },
    "errorBreakdown": {
      "OptimisticLockException": 1
    }
  },
  "environment": {
    "javaVersion": "21.0.1",
    "os": "Linux 6.5.0",
    "cpu": "Intel i7-12700K",
    "memory": "32GB",
    "database": "PostgreSQL 16",
    "network": "localhost"
  }
}
```

### Visualization

Key charts for dashboards:
1. **Latency Heatmap** - Time vs Percentile (HdrHistogram native)
2. **Throughput Over Time** - Requests/second per interval
3. **Error Rate Over Time** - Errors/second per interval
4. **Latency Distribution** - Histogram (log scale X)
5. **Comparison Bar Chart** - Strategy vs Metric

## Common Pitfalls

| Pitfall | Impact | Fix |
|---------|--------|-----|
| Measuring during warm-up | Inflated latency | Discard first 30s/10% |
| Calculating p99 from averages | Wrong by orders of magnitude | Use HdrHistogram |
| Too few samples | Unreliable tail percentiles | 100k+ requests |
| Coordinated omission | Missed latency during GC | Use HdrHistogram's auto-correction |
| Shared infrastructure noise | Unreproducible results | Dedicated test environment |
| Single run | No confidence intervals | Multiple runs, report median |

## Tools

- **HdrHistogram** - Core percentile calculation
- **JMH** - Microbenchmarks (JIT, throughput)
- **async-profiler** - CPU/memory profiling
- **perf** - Linux system profiling
- **pg_stat_statements** - Database query analysis
- **Prometheus/Grafana** - Time-series visualization

## Reproducibility Checklist

- [ ] Exact software versions recorded (Java, DB, OS)
- [ ] Hardware specs documented
- [ ] Network topology described
- [ ] Warm-up methodology specified
- [ ] Measurement duration stated
- [ ] Raw histogram data archived
- [ ] Configuration version controlled
- [ ] Random seeds fixed (if applicable)