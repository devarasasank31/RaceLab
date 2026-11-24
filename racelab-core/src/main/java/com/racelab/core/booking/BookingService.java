package com.racelab.core.booking;

import com.racelab.core.domain.Ticket;

/**
 * Interface for ticket booking services with different concurrency strategies.
 */
public interface BookingService {

    /**
     * Attempt to book a ticket.
     *
     * @param eventId the event identifier
     * @param userId the user identifier
     * @param quantity number of tickets to book
     * @return booking result with success status and details
     */
    BookingResult book(String eventId, String userId, int quantity);

    /**
     * Get the strategy type this service implements.
     */
    ConcurrencyStrategyType getStrategyType();

    /**
     * Reset the ticket for a new test run.
     */
    void resetTicket(String eventId);
}