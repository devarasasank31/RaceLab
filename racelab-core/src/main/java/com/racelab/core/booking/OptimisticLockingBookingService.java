package com.racelab.core.booking;

import com.racelab.core.domain.Ticket;
import com.racelab.core.domain.TicketStatus;
import com.racelab.core.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * OPTIMISTIC LOCKING: Uses JPA @Version for conflict detection.
 * 
 * This implementation relies on the @Version field in the Ticket entity.
 * When multiple threads try to update the same ticket, only one succeeds
 * and the others get OptimisticLockingFailureException, which we retry.
 * 
 * Expected behavior under 1000 concurrent requests for 1 ticket:
 * - Exactly 1 successful booking
 * - 999 retries/failures with OptimisticLockingFailureException
 * - Database state: consistent
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimisticLockingBookingService implements BookingService {

    private final TicketRepository ticketRepository;

    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 10;

    @Override
    @Transactional
    public BookingResult book(String eventId, String userId, int quantity) {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                // Read with optimistic lock (version check happens on save)
                Ticket ticket = ticketRepository.findByEventId(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

                if (ticket.getAvailableQuantity() < quantity) {
                    return BookingResult.failure(
                        "Not enough tickets available",
                        ticket.getAvailableQuantity(),
                        getStrategyType().name()
                    );
                }

                // Update - version checked on save
                ticket.setAvailableQuantity(ticket.getAvailableQuantity() - quantity);
                ticket.setBookedQuantity(ticket.getBookedQuantity() + quantity);
                ticket.setStatus(ticket.getAvailableQuantity() == 0 ? TicketStatus.SOLD_OUT : TicketStatus.PARTIALLY_BOOKED);
                ticket.setBookingReference("BOOK-" + UUID.randomUUID().toString().substring(0, 8));
                ticket.setBookedBy(userId);
                ticket.setBookedAt(java.time.Instant.now());

                ticketRepository.saveAndFlush(ticket); // Flush to trigger version check

                return BookingResult.success(
                    ticket.getBookingReference(),
                    ticket.getAvailableQuantity(),
                    getStrategyType().name()
                );

            } catch (OptimisticLockingFailureException e) {
                attempts++;
                log.debug("Optimistic lock conflict, retry {}/{}", attempts, MAX_RETRIES);
                if (attempts >= MAX_RETRIES) {
                    return BookingResult.failure(
                        "Max retries exceeded due to concurrent modifications",
                        0,
                        getStrategyType().name()
                    );
                }
                // Small delay before retry
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return BookingResult.failure("Interrupted", 0, getStrategyType().name());
                }
            }
        }
        return BookingResult.failure("Max retries exceeded", 0, getStrategyType().name());
    }

    @Override
    public ConcurrencyStrategyType getStrategyType() {
        return ConcurrencyStrategyType.OPTIMISTIC_LOCKING;
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
            ticket.setVersion(0L); // Reset version
            ticketRepository.save(ticket);
        });
    }
}