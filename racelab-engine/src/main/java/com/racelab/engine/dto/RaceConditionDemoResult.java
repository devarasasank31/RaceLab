package com.racelab.engine.dto;

import com.racelab.core.booking.StrategyComparisonResult;
import com.racelab.core.domain.ConcurrencyStrategyType;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result DTO for race condition demonstration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceConditionDemoResult {

    private String eventId;
    private int totalTickets;
    private int concurrentRequests;
    private Instant timestamp;
    private VulnerableResult vulnerableResult;
    private Map<ConcurrencyStrategyType, StrategyResult> strategyResults;
    private ComparisonSummary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VulnerableResult {
        private int successfulBookings;
        private int failedBookings;
        private int finalAvailableQuantity;
        private int finalBookedQuantity;
        private boolean oversold;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StrategyResult {
        private ConcurrencyStrategyType strategyType;
        private int successfulBookings;
        private int failedBookings;
        private int finalAvailableQuantity;
        private int finalBookedQuantity;
        private boolean consistent;
        private boolean oversold;
        private String description;
        private double successRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComparisonSummary {
        private String vulnerableDescription;
        private Map<ConcurrencyStrategyType, String> strategyDescriptions;
        private List<String> consistentStrategies;
        private List<String> oversoldStrategies;
    }
}