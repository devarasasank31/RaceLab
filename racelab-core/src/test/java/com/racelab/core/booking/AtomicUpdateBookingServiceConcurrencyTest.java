package com.racelab.core.booking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for atomic update booking service.
 * Verifies that conditional UPDATE prevents race conditions.
 */
class AtomicUpdateBookingServiceConcurrencyTest extends BookingServiceTestBase {

    @Test
    void atomicUpdate_OneTicket100ConcurrentRequests_ExactlyOneSuccess() {
        // Given: 1 ticket, 100 concurrent requests
        createTicket(1);

        // When: Run concurrent booking with atomic update
        ConcurrencyTestResult result = runConcurrentTest(atomicService, 100);

        // Then: Exactly one success, database consistent
        assertThat(result.completed()).isTrue();
        assertThat(result.successes() + result.failures()).isEqualTo(100);
        assertThat(result.successes()).isEqualTo(1);
        assertThat(result.failures()).isEqualTo(99);
        assertThat(result.finalAvailable()).isZero();
        assertThat(result.finalBooked()).isEqualTo(1);

        System.out.printf("Atomic Update: successes=%d, failures=%d, finalAvailable=%d, finalBooked=%d%n",
            result.successes(), result.failures(), result.finalAvailable(), result.finalBooked());
    }
}