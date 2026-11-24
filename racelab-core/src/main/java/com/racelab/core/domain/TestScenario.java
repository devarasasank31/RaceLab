package com.racelab.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Map;

/**
 * A test scenario defining the parameters for a concurrency test.
 */
@Entity
@Table(name = "test_scenarios", indexes = {
    @Index(name = "idx_test_scenario_name", columnList = "name"),
    @Index(name = "idx_test_scenario_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"requestHeaders", "requestPayload"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TestScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    @Min(1)
    @Max(10000)
    @Column(name = "concurrent_users", nullable = false)
    private Integer concurrentUsers;

    @Min(1)
    @Max(1000000)
    @Column(name = "total_requests")
    private Integer totalRequests;

    @Min(1)
    @Max(3600)
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Min(0)
    @Max(300)
    @Column(name = "ramp_up_seconds")
    private Integer rampUpSeconds;

    @NotBlank
    @Size(max = 2048)
    @Column(name = "target_endpoint", nullable = false)
    private String targetEndpoint;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false)
    private HttpMethod httpMethod;

    @Column(name = "request_headers", columnDefinition = "jsonb")
    private Map<String, String> requestHeaders;

    @Column(name = "request_payload", columnDefinition = "text")
    private String requestPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "concurrency_strategy", nullable = false)
    @Builder.Default
    private ConcurrencyStrategyType concurrencyStrategy = ConcurrencyStrategyType.NONE;

    @Column(name = "strategy_config", columnDefinition = "jsonb")
    private Map<String, Object> strategyConfig;

    @Min(1)
    @Max(300)
    @Column(name = "request_timeout_seconds")
    @Builder.Default
    private Integer requestTimeoutSeconds = 30;

    @Min(0)
    @Max(10)
    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public boolean isDurationBased() {
        return durationSeconds != null && durationSeconds > 0;
    }

    public boolean isRequestCountBased() {
        return totalRequests != null && totalRequests > 0;
    }

    public void validateConfiguration() {
        if (!isDurationBased() && !isRequestCountBased()) {
            throw new IllegalStateException("Either durationSeconds or totalRequests must be specified");
        }
        if (rampUpSeconds != null && rampUpSeconds > 0) {
            int effectiveDuration = durationSeconds != null ? durationSeconds : 60;
            if (rampUpSeconds >= effectiveDuration) {
                throw new IllegalStateException("Ramp-up period must be less than test duration");
            }
        }
    }
}