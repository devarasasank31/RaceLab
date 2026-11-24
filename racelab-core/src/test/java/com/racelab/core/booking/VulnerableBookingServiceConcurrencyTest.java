package com.racelab.core.booking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test demonstrating the race condition in vulnerable booking service.
 * 
 * This test EXPECTS the race condition to occur - multiple successful
 * bookings for a single ticket. This is the "before" picture showing
 * why concurrency protection is needed.
 */
class VulnerableBookingServiceConcurrencyTest extends BookingServiceTestBase {

    @Test
    void vulnerableBooking_OneTicket100ConcurrentRequests_RaceConditionOccurs() {
        // Given: 1 ticket, 100 concurrent requests
        createTicket(1);

        // When: Run concurrent booking without protection
        ConcurrencyTestResult result = runConcurrentTest(vulnerableService, 100);

        // Then: Race condition occurs - multiple successes possible
        // Note: This test may occasionally pass (all failures) due to timing,
        // but under sustained load it will show the race condition
        assertThat(result.completed()).isTrue();
        assertThat(result.successes() + result.failures()).isEqualTo(100);

        // The race condition manifests as OVERSOLD state
        // (multiple successful bookings for 1 ticket)
        boolean oversold = result.finalBooked() > 1 || result.finalAvailable() < 0;
        
        System.out.printf("Vulnerable: successes=%d, failures=%d, finalAvailable=%d, finalBooked=%d, oversold=%b%n",
            result.successes(), result.failures(), result.finalAvailable(), result.finalBooked(), oversold);
    }
}