package com.racelab.core.distributed;

import com.racelab.core.booking.BookingResult;
import com.racelab.core.booking.BookingService;
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
 * REDIS DISTRIBUTED LOCK: Uses Redis for cross-service coordination.
 * 
 * This implementation acquires a distributed lock via Redis before
 * performing the booking operation. Useful when multiple service
 * instances need to coordinate.
 * 
 * Expected behavior under 1000 concurrent requests for 1 ticket:
 * - Exactly 1 successful booking (if lock works correctly)
 * - 999 fail to acquire lock
 * - Database state: consistent
 * 
 * Trade-offs:
 * - Additional infrastructure (Redis)
 * - Network latency for lock acquisition
 * - Single point of failure (Redis)
 * - Clock synchronization issues possible
 * - Redlock algorithm has known safety concerns (use Redisson for production)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisDistributedLockBookingService implements BookingService {

    private final RedisDistributedLock distributedLock;
    private final TicketRepository ticketRepository;

    private static final long LOCK_TIMEOUT_MS = 5000;
    private static final long ACQUIRE_TIMEOUT_MS = 10000;

    @Override
    @Transactional
    public BookingResult book(String eventId, String userId, int quantity) {
        // Try to acquire distributed lock
        var lockHandle = distributedLock.tryLock(
            "booking:" + eventId,
            LOCK_TIMEOUT_MS,
            ACQUIRE_TIMEOUT_MS,
            null // No cancellation token for now
        );

        if (lockHandle == null) {
            // Check if ticket exists to provide better error message
            var ticketOpt = ticketRepository.findByEventId(eventId);
            int available = ticketOpt.map(Ticket::getAvailableQuantity).orElse(0);
            return BookingResult.failure(
                "Could not acquire distributed lock",
                available,
                getStrategyType().name()
            );
        }

        try (lockHandle) {
            // Lock acquired - perform booking
            Ticket ticket = ticketRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

            if (ticket.getAvailableQuantity() < quantity) {
                return BookingResult.failure(
                    "Not enough tickets available",
                    ticket.getAvailableQuantity(),
                    getStrategyType().name()
                );
            }

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

        } catch (Exception e) {
            log.error("Booking failed for event {}", eventId, e);
            return BookingResult.failure(
                "Booking failed: " + e.getMessage(),
                0,
                getStrategyType().name()
            );
        }
    }

    @Override
    public ConcurrencyStrategyType getStrategyType() {
        return ConcurrencyStrategyType.REDIS_DISTRIBUTED_LOCK;
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