package com.racelab.core.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Map;

/**
 * An execution instance of a test scenario.
 */
@Entity
@Table(name = "test_executions", indexes = {
    @Index(name = "idx_test_execution_scenario", columnList = "scenario_id"),
    @Index(name = "idx_test_execution_status", columnList = "status"),
    @Index(name = "idx_test_execution_started_at", columnList = "started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private TestScenario scenario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TestStatus status = TestStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "executed_requests")
    @Builder.Default
    private Long executedRequests = 0L;

    @Column(name = "successful_requests")
    @Builder.Default
    private Long successfulRequests = 0L;

    @Column(name = "failed_requests")
    @Builder.Default
    private Long failedRequests = 0L;

    @Column(name = "configuration_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> configurationSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public long getDurationMillis() {
        if (startedAt == null) return 0;
        Instant end = completedAt != null ? completedAt : cancelledAt != null ? cancelledAt : Instant.now();
        return java.time.Duration.between(startedAt, end).toMillis();
    }

    public double getSuccessRate() {
        if (executedRequests == 0) return 0.0;
        return (double) successfulRequests / executedRequests * 100.0;
    }

    public double getErrorRate() {
        if (executedRequests == 0) return 0.0;
        return (double) failedRequests / executedRequests * 100.0;
    }

    public boolean isTerminal() {
        return status == TestStatus.COMPLETED
            || status == TestStatus.CANCELLED
            || status == TestStatus.FAILED
            || status == TestStatus.COMPLETED_WITH_VIOLATIONS;
    }
}