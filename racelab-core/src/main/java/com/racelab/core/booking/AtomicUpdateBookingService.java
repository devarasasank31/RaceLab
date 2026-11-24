package com.racelab.core.booking;

import com.racelab.core.domain.Ticket;
import com.racelab.core.domain.TicketStatus;
import com.racelab.core.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * ATOMIC UPDATE: Uses single conditional UPDATE statement.
 * 
 * This implementation performs the entire check-and-update in a single
 * atomic database operation. No application-level locking needed.
 * 
 * Expected behavior under 1000 concurrent requests for 1 ticket:
 * - Exactly 1 successful booking (1 row updated)
 * - 999 immediate failures (0 rows updated)
 * - Database state: consistent
 * - Trade-off: Limited to simple operations, complex logic requires stored procedures
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AtomicUpdateBookingService implements BookingService {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public BookingResult book(String eventId, String userId, int quantity) {
        // First get the ticket to know its ID
        var ticketOpt = ticketRepository.findByEventId(eventId);
        if (ticketOpt.isEmpty()) {
            return BookingResult.failure("Event not found: " + eventId, 0, getStrategyType().name());
        }
        Ticket ticket = ticketOpt.get();

        // Atomic conditional update
        int updated = ticketRepository.bookTicketAtomically(
            ticket.getId(),
            quantity,
            "BOOK-" + UUID.randomUUID().toString().substring(0, 8),
            userId,
            "idem-" + UUID.randomUUID().toString().substring(0, 8)
        );

        if (updated > 0) {
            // Reload to get updated state
            Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
            return BookingResult.success(
                updatedTicket.getBookingReference(),
                updatedTicket.getAvailableQuantity(),
                getStrategyType().name()
            );
        } else {
            // Reload to get current availability
            Ticket currentTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
            return BookingResult.failure(
                "Not enough tickets available",
                currentTicket.getAvailableQuantity(),
                getStrategyType().name()
            );
        }
    }

    @Override
    public ConcurrencyStrategyType getStrategyType() {
        return ConcurrencyStrategyType.ATOMIC_UPDATE;
    }

    @Override
    @Transactional
    public void resetTicket(String eventId) {
        ticketRepository.findByEventId(eventId).ifPresent(ticket -> {
            ticket.setAvailableQuantity(ticket.getTotalQuantity());
            ticket.setBookedQuantity(0);
            ticket.setStatus(TicketStatus.AVAILABLE);
            ticket.setBookingReference(null);
            ticket.setBookedBy(null);
            ticket.setBookedAt(null);
            ticket.setIdempotencyKey(null);
            ticketRepository.save(ticket);
        });
    }
}