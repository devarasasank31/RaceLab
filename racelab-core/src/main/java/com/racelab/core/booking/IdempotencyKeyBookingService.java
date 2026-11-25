package com.racelab.core.booking;

import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.core.domain.Ticket;
import com.racelab.core.domain.TicketStatus;
import com.racelab.core.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * IDEMPOTENCY KEY: Client-provided deduplication keys.
 * 
 * This implementation uses a unique constraint on the idempotency_key column.
 * The client generates a unique key for each booking attempt. Duplicate keys
 * are rejected by the database unique constraint.
 * 
 * Expected behavior under 1000 concurrent requests for 1 ticket:
 * - If all requests use SAME idempotency key: Exactly 1 success, 999 return existing
 * - If all requests use DIFFERENT idempotency keys: Same as vulnerable (overselling)
 * - Database state: consistent (if keys managed correctly)
 * - Trade-off: Requires client cooperation, key management complexity
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyKeyBookingService implements BookingService {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public BookingResult book(String eventId, String userId, int quantity) {
        // In real usage, client provides idempotency key
        // For demo, we generate one per user+event combination
        String idempotencyKey = "idem-" + eventId + "-" + userId;

        // Check if already processed
        var existing = ticketRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Ticket existingTicket = existing.get();
            return BookingResult.builder()
                .success(true)
                .bookingReference(existingTicket.getBookingReference())
                .message("Duplicate request - returning existing booking")
                .availableAfterBooking(existingTicket.getAvailableQuantity())
                .timestamp(java.time.Instant.now())
                .strategyType(getStrategyType().name())
                .build();
        }

        Ticket ticket = ticketRepository.findByEventId(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        if (ticket.getAvailableQuantity() < quantity) {
            return BookingResult.failure(
                "Not enough tickets available",
                ticket.getAvailableQuantity(),
                getStrategyType().name()
            );
        }

        try {
            // Try to book with idempotency key
            ticket.setAvailableQuantity(ticket.getAvailableQuantity() - quantity);
            ticket.setBookedQuantity(ticket.getBookedQuantity() + quantity);
            ticket.setStatus(ticket.getAvailableQuantity() == 0 ? TicketStatus.SOLD_OUT : TicketStatus.PARTIALLY_BOOKED);
            ticket.setBookingReference("BOOK-" + UUID.randomUUID().toString().substring(0, 8));
            ticket.setBookedBy(userId);
            ticket.setBookedAt(java.time.Instant.now());
            ticket.setIdempotencyKey(idempotencyKey);

            ticketRepository.saveAndFlush(ticket);

            return BookingResult.success(
                ticket.getBookingReference(),
                ticket.getAvailableQuantity(),
                getStrategyType().name()
            );

        } catch (DataIntegrityViolationException e) {
            // Another request with same idempotency key won
            var existingTicket = ticketRepository.findByIdempotencyKey(idempotencyKey);
            if (existingTicket.isPresent()) {
                Ticket t = existingTicket.get();
                return BookingResult.builder()
                    .success(true)
                    .bookingReference(t.getBookingReference())
                    .message("Duplicate request - returning existing booking")
                    .availableAfterBooking(t.getAvailableQuantity())
                    .timestamp(java.time.Instant.now())
                    .strategyType(getStrategyType().name())
                    .build();
            }
            return BookingResult.failure("Concurrent booking conflict", 0, getStrategyType().name());
        }
    }

    @Override
    public ConcurrencyStrategyType getStrategyType() {
        return ConcurrencyStrategyType.IDEMPOTENCY_KEY;
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