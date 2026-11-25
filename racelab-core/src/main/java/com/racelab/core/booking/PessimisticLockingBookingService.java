package com.racelab.core.booking;

import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.core.domain.Ticket;
import com.racelab.core.domain.TicketStatus;
import com.racelab.core.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * PESSIMISTIC LOCKING: Uses SELECT FOR UPDATE to acquire database row lock.
 * 
 * This implementation acquires an exclusive lock on the ticket row before
 * reading and updating. Other threads block until the lock is released.
 * 
 * Expected behavior under 1000 concurrent requests for 1 ticket:
 * - Exactly 1 successful booking
 * - 999 threads block until lock released, then fail (no tickets left)
 * - Database state: consistent
 * - Trade-off: lock contention, potential deadlocks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PessimisticLockingBookingService implements BookingService {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public BookingResult book(String eventId, String userId, int quantity) {
        // Acquire pessimistic lock (SELECT FOR UPDATE)
        Ticket ticket = ticketRepository.findByEventIdForUpdate(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        if (ticket.getAvailableQuantity() < quantity) {
            return BookingResult.failure(
                "Not enough tickets available",
                ticket.getAvailableQuantity(),
                getStrategyType().name()
            );
        }

        // Update while holding lock
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
        return ConcurrencyStrategyType.PESSIMISTIC_LOCKING;
    }

    @Override
    @Transactional
    public void resetTicket(String eventId) {
        // Also use pessimistic lock for reset
        ticketRepository.findByEventIdForUpdate(eventId).ifPresent(ticket -> {
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