package com.racelab.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Individual request result within a test execution.
 */
@Entity
@Table(name = "request_results", indexes = {
    @Index(name = "idx_request_result_execution", columnList = "execution_id"),
    @Index(name = "idx_request_result_timestamp", columnList = "timestamp"),
    @Index(name = "idx_request_result_status", columnList = "status"),
    @Index(name = "idx_request_result_latency", columnList = "latency_ms")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RequestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private TestExecution execution;

    @Column(name = "request_number")
    private Long requestNumber;

    @Column(name = "thread_name", length = 100)
    private String threadName;

    @Column(name = "virtual_thread")
    private Boolean virtualThread;

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "response_size_bytes")
    private Long responseSizeBytes;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}