package com.racelab.core.booking;

import com.racelab.core.domain.ConcurrencyStrategyType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive comparison test for all concurrency strategies.
 * Runs the same scenario with each strategy and verifies correctness.
 */
class StrategyComparisonConcurrencyTest extends BookingServiceTestBase {

    @Test
    void allStrategies_OneTicket100Requests_AllPreventOverselling() {
        // Given: 1 ticket, 100 concurrent requests per strategy
        Map<ConcurrencyStrategyType, BookingService> services = Map.of(
            ConcurrencyStrategyType.OPTIMISTIC_LOCKING, optimisticService,
            ConcurrencyStrategyType.PESSIMISTIC_LOCKING, pessimisticService,
            ConcurrencyStrategyType.ATOMIC_UPDATE, atomicService,
            ConcurrencyStrategyType.IDEMPOTENCY_KEY, idempotencyService
        );

        // Test each strategy
        for (var entry : services.entrySet()) {
            ConcurrencyStrategyType strategy = entry.getKey();
            BookingService service = entry.getValue();

            // Fresh ticket for each strategy
            createTicket(1);

            // Run test
            ConcurrencyTestResult result = runConcurrentTest(service, 100);

            // Verify
            assertThat(result.completed()).as("%s: test completed", strategy).isTrue();
            assertThat(result.successes() + result.failures()).as("%s: all requests accounted", strategy).isEqualTo(100);
            
            // For correct strategies: exactly 1 success, rest failures
            // For IDEMPOTENCY_KEY with different users: may have race condition
            if (strategy != ConcurrencyStrategyType.IDEMPOTENCY_KEY) {
                assertThat(result.successes()).as("%s: exactly 1 success", strategy).isEqualTo(1);
                assertThat(result.failures()).as("%s: 99 failures", strategy).isEqualTo(99);
                assertThat(result.finalAvailable()).as("%s: 0 available", strategy).isZero();
                assertThat(result.finalBooked()).as("%s: 1 booked", strategy).isEqualTo(1);
            } else {
                // Idempotency key with different users per request - behaves like vulnerable
                // But same user retries are deduplicated
                System.out.printf("Strategy %s: successes=%d, failures=%d, finalAvailable=%d, finalBooked=%d%n",
                    strategy, result.successes(), result.failures(), result.finalAvailable(), result.finalBooked());
            }

            // No overselling for any strategy
            assertThat(result.finalBooked()).as("%s: no overselling", strategy).isLessThanOrEqualTo(1);
            assertThat(result.finalAvailable()).as("%s: no negative available", strategy).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void allStrategies_MultipleTickets_CorrectBookingCount() {
        Map<ConcurrencyStrategyType, BookingService> services = Map.of(
            ConcurrencyStrategyType.OPTIMISTIC_LOCKING, optimisticService,
            ConcurrencyStrategyType.PESSIMISTIC_LOCKING, pessimisticService,
            ConcurrencyStrategyType.ATOMIC_UPDATE, atomicService,
            ConcurrencyStrategyType.IDEMPOTENCY_KEY, idempotencyService
        );

        for (var entry : services.entrySet()) {
            ConcurrencyStrategyType strategy = entry.getKey();
            BookingService service = entry.getValue();

            // Given: 10 tickets
            createTicket(10);

            // When: 500 concurrent requests
            ConcurrencyTestResult result = runConcurrentTest(service, 500);

            // Then: Exactly 10 successes (one per ticket)
            assertThat(result.completed()).as("%s: test completed", strategy).isTrue();
            assertThat(result.successes() + result.failures()).as("%s: all requests accounted", strategy).isEqualTo(500);
            assertThat(result.successes()).as("%s: exactly 10 successes", strategy).isEqualTo(10);
            assertThat(result.failures()).as("%s: 490 failures", strategy).isEqualTo(490);
            assertThat(result.finalAvailable()).as("%s: 0 available", strategy).isZero();
            assertThat(result.finalBooked()).as("%s: 10 booked", strategy).isEqualTo(10);
            assertThat(result.finalBooked()).as("%s: no overselling", strategy).isLessThanOrEqualTo(10);

            System.out.printf("Strategy %s (10 tickets): successes=%d, failures=%d%n",
                strategy, result.successes(), result.failures());
        }
    }
}