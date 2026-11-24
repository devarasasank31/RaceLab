package com.racelab.core.domain;

/**
 * Status of a ticket.
 */
public enum TicketStatus {
    /** Tickets are available for booking. */
    AVAILABLE,
    /** Some tickets have been booked. */
    PARTIALLY_BOOKED,
    /** All tickets have been booked. */
    SOLD_OUT,
    /** Booking failed or was cancelled. */
    CANCELLED
}