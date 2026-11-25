package com.racelab.core.detection;

import com.racelab.core.domain.TestExecution;
import com.racelab.core.domain.Ticket;
import com.racelab.core.domain.TicketStatus;
import com.racelab.core.repository.TicketRepository;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks for duplicate bookings - the same ticket booked multiple times.
 */
@RequiredArgsConstructor
public class DuplicateBookingChecker implements InvariantChecker {

    private final TicketRepository ticketRepository;

    @Override
    public String getInvariantName() {
        return "NO_DUPLICATE_BOOKINGS";
    }

    @Override
    public String getDescription() {
        return "Ensures each ticket can only be booked once. No two successful bookings should exist for the same ticket.";
    }

    @Override
    public List<InvariantViolation> check(TestExecution execution) {
        List<InvariantViolation> violations = new ArrayList<>();

        // Find all tickets that were booked during this execution
        List<Ticket> bookedTickets = ticketRepository.findByBookingReferenceNotNull();

        for (Ticket ticket : bookedTickets) {
            // Check if multiple bookings reference the same ticket
            long bookingCount = ticketRepository.countByEventIdAndStatusNot(ticket.getEventId(), TicketStatus.AVAILABLE);
            if (bookingCount > 1) {
                violations.add(InvariantViolation.builder()
                    .invariantName(getInvariantName())
                    .description("Multiple bookings detected for event: " + ticket.getEventId())
                    .expectedValue(1)
                    .actualValue(bookingCount)
                    .severity(InvariantViolation.Severity.CRITICAL)
                    .type(InvariantViolation.ViolationType.DUPLICATE_OPERATION)
                    .context("Event: " + ticket.getEventId() + ", Ticket ID: " + ticket.getId())
                    .build());
            }
        }

        return violations;
    }

    @Override
    public boolean requiresDatabaseAccess() {
        return true;
    }
}