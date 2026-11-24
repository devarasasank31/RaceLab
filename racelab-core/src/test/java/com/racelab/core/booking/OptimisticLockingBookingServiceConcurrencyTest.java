package com.racelab.core.booking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for optimistic locking booking service.
 * Verifies that JPA @Version prevents race conditions.
 */
class OptimisticLockingBookingServiceConcurrencyTest extends BookingServiceTestBase {

    @Test
    void optimisticLocking_OneTicket100ConcurrentRequests_ExactlyOneSuccess() {
        // Given: 1 ticket, 100 concurrent requests
        createTicket(1);

        // When: Run concurrent booking with optimistic locking
        ConcurrencyTestResult result = runConcurrentTest(optimisticService, 100);

        // Then: Exactly one success, database consistent
        assertThat(result.completed()).isTrue();
        assertThat(result.successes() + result.failures()).isEqualTo(100);
        assertThat(result.successes()).isEqualTo(1);
        assertThat(result.failures()).isEqualTo(99);
        assertThat(result.finalAvailable()).isZero();
        assertThat(result.finalBooked()).isEqualTo(1);

        System.out.printf("Optimistic Locking: successes=%d, failures=%d, finalAvailable=%d, finalBooked=%d%n",
            result.successes(), result.failures(), result.finalAvailable(), result.finalBooked());
    }

    @Test
    void optimisticLocking_MultipleTickets_AllBookedCorrectly() {
        // Given: 5 tickets, 200 concurrent requests (40 per ticket on average)
        createTicket(5);

        // When: Run concurrent booking with optimistic locking
        ConcurrencyTestResult result = runConcurrentTest(optimisticService, 200);

        // Then: Exactly 5 successes (one per ticket)
        assertThat(result.completed()).isTrue();
        assertThat(result.successes()).isEqualTo(5);
        assertThat(result.failures()).isEqualTo(195);
        assertThat(result.finalAvailable()).isZero();
        assertThat(result.finalBooked()).isEqualTo(5);

        System.out.printf("Optimistic Locking (5 tickets): successes=%d, failures=%d%n",
            result.successes(), result.failures());
    }
}