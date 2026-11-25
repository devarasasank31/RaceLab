package com.racelab.core.booking;

import com.racelab.core.domain.ConcurrencyStrategyType;
import lombok.*;

import java.util.List;

/**
 * Result of a strategy comparison experiment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class StrategyComparisonResult {

    private String eventId;
    private int totalQuantity;
    private int concurrentRequests;
    private List<StrategyResult> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    @EqualsAndHashCode
    public static class StrategyResult {
        private ConcurrencyStrategyType strategyType;
        private int totalRequests;
        private int successfulBookings;
        private int failedBookings;
        private int finalAvailableQuantity;
        private int finalBookedQuantity;
        private int totalQuantity;
        private List<BookingResult> results;
        private boolean consistent;

        public double getSuccessRate() {
            if (totalRequests == 0) return 0.0;
            return (double) successfulBookings / totalRequests * 100.0;
        }

        public boolean isOversold() {
            return finalBookedQuantity > totalQuantity || finalAvailableQuantity < 0;
        }
    }
}