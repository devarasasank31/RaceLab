package com.racelab.core.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Map;

/**
 * Aggregated results of a test execution.
 */
@Entity
@Table(name = "test_results", indexes = {
    @Index(name = "idx_test_result_execution", columnList = "execution_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false, unique = true)
    private TestExecution execution;

    @Column(name = "total_requests")
    private Long totalRequests;

    @Column(name = "successful_requests")
    private Long successfulRequests;

    @Column(name = "failed_requests")
    private Long failedRequests;

    @Column(name = "requests_per_second")
    private Double requestsPerSecond;

    @Column(name = "avg_latency_ms")
    private Double avgLatencyMs;

    @Column(name = "median_latency_ms")
    private Double medianLatencyMs;

    @Column(name = "p95_latency_ms")
    private Double p95LatencyMs;

    @Column(name = "p99_latency_ms")
    private Double p99LatencyMs;

    @Column(name = "min_latency_ms")
    private Double minLatencyMs;

    @Column(name = "max_latency_ms")
    private Double maxLatencyMs;

    @Column(name = "std_deviation_ms")
    private Double stdDeviationMs;

    @Column(name = "error_rate_percent")
    private Double errorRatePercent;

    @Column(name = "throughput_mbps")
    private Double throughputMbps;

    @Column(name = "latency_histogram", columnDefinition = "bytea")
    private byte[] latencyHistogram;

    @Column(name = "error_breakdown", columnDefinition = "jsonb")
    private Map<String, Long> errorBreakdown;

    @Column(name = "status_code_distribution", columnDefinition = "jsonb")
    private Map<Integer, Long> statusCodeDistribution;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;
}