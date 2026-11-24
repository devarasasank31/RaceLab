package com.racelab.core.booking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for pessimistic locking booking service.
 * Verifies that SELECT FOR UPDATE prevents race conditions.
 */
class PessimisticLockingBookingServiceConcurrencyTest extends BookingServiceTestBase {

    @Test
    void pessimisticLocking_OneTicket100ConcurrentRequests_ExactlyOneSuccess() {
        // Given: 1 ticket, 100 concurrent requests
        createTicket(1);

        // When: Run concurrent booking with pessimistic locking
        ConcurrencyTestResult result = runConcurrentTest(pessimisticService, 100);

        // Then: Exactly one success, database consistent
        assertThat(result.completed()).isTrue();
        assertThat(result.successes() + result.failures()).isEqualTo(100);
        assertThat(result.successes()).isEqualTo(1);
        assertThat(result.failures()).isEqualTo(99);
        assertThat(result.finalAvailable()).isZero();
        assertThat(result.finalBooked()).isEqualTo(1);

        System.out.printf("Pessimistic Locking: successes=%d, failures=%d, finalAvailable=%d, finalBooked=%d%n",
            result.successes(), result.failures(), result.finalAvailable(), result.finalBooked());
    }
}