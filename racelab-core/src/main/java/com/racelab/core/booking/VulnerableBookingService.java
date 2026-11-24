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
 * VULNERABLE: No concurrency protection - demonstrates race conditions.
 * 
 * This implementation reads the ticket, checks availability, then updates.
 * Under concurrent access, multiple threads can read the same available count
 * and all succeed in booking, leading to overselling.
 * 
 * Expected behavior under 1000 concurrent requests for 1 ticket:
 * - Multiple successful bookings (race condition)
 * - Database state: negative available quantity possible
 * - Data corruption
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VulnerableBookingService implements BookingService {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public BookingResult book(String eventId, String userId, int quantity) {
        // VULNERABLE: Read without locking
        Ticket ticket = ticketRepository.findByEventId(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        // VULNERABLE: Check availability without locking
        if (ticket.getAvailableQuantity() < quantity) {
            return BookingResult.failure(
                "Not enough tickets available",
                ticket.getAvailableQuantity(),
                getStrategyType().name()
            );
        }

        // VULNERABLE: Update without atomic check
        ticket.setAvailableQuantity(ticket.getAvailableQuantity() - quantity);
        ticket.setBookedQuantity(ticket.getBookedQuantity() + quantity);
        ticket.setStatus(ticket.getAvailableQuantity() == 0 ? TicketStatus.SOLD_OUT : TicketStatus.PARTIALLY_BOOKED);
        ticket.setBookingReference("BOOK-" + UUID.randomUUID().toString().substring(0, 8));
        ticket.setBookedBy(userId);
        ticket.setBookedAt(java.time.Instant.now());

        ticketRepository.save(ticket);

        return BookingResult.success(
            ticket.getBookingReference(),
            ticket.getAvailableQuantity(),
            getStrategyType().name()
        );
    }

    @Override
    public ConcurrencyStrategyType getStrategyType() {
        return ConcurrencyStrategyType.NONE;
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