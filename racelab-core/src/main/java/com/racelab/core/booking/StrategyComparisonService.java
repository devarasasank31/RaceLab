package com.racelab.core.booking;

import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.core.domain.Ticket;
import com.racelab.core.domain.TicketStatus;
import com.racelab.core.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for running strategy comparison experiments.
 * Executes the same booking scenario with different concurrency strategies
 * and collects comparative results.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StrategyComparisonService {

    private final Map<ConcurrencyStrategyType, BookingService> strategyServices;
    private final TicketRepository ticketRepository;

    public StrategyComparisonResult runComparison(
            String eventId,
            int totalQuantity,
            int concurrentRequests,
            int quantityPerRequest) {

        log.info("Starting strategy comparison for event: {}, {} requests", eventId, concurrentRequests);

        List<StrategyComparisonResult.StrategyResult> results = new ArrayList<>();

        for (ConcurrencyStrategyType strategyType : ConcurrencyStrategyType.values()) {
            if (strategyType == ConcurrencyStrategyType.REDIS_DISTRIBUTED_LOCK) {
                log.warn("Skipping Redis distributed lock - not implemented in core");
                continue;
            }

            // Create fresh ticket for this strategy
            createTestTicket(eventId, totalQuantity);

            BookingService service = strategyServices.get(strategyType);
            if (service == null) {
                log.warn("No service for strategy: {}", strategyType);
                continue;
            }

            // Run concurrent booking test
            StrategyComparisonResult.StrategyResult result = runConcurrentTest(
                service, eventId, concurrentRequests, quantityPerRequest
            );
            results.add(result);

            log.info("Strategy {} completed: {} successes, {} failures",
                strategyType, result.getSuccessfulBookings(), result.getFailedBookings());
        }

        return new StrategyComparisonResult(eventId, totalQuantity, concurrentRequests, results);
    }

    private StrategyComparisonResult.StrategyResult runConcurrentTest(
            BookingService service,
            String eventId,
            int concurrentRequests,
            int quantityPerRequest) {

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<BookingResult> allResults = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < concurrentRequests; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    BookingResult result = service.book(eventId, "user-" + userId, quantityPerRequest);
                    allResults.add(result);
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.debug("Booking failed for user {}: {}", userId, e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        try {
            endLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdownNow();

        // Check final database state
        Ticket finalTicket = ticketRepository.findByEventId(eventId).orElse(null);
        int finalAvailable = finalTicket != null ? finalTicket.getAvailableQuantity() : -1;
        int finalBooked = finalTicket != null ? finalTicket.getBookedQuantity() : -1;

        return StrategyComparisonResult.StrategyResult.builder()
            .strategyType(service.getStrategyType())
            .totalRequests(concurrentRequests)
            .successfulBookings(successCount.get())
            .failedBookings(failureCount.get())
            .finalAvailableQuantity(finalAvailable)
            .finalBookedQuantity(finalBooked)
            .results(allResults)
            .consistent(finalBooked <= totalQuantity && finalAvailable >= 0)
            .build();
    }

    @Transactional
    public void createTestTicket(String eventId, int totalQuantity) {
        Ticket ticket = Ticket.builder()
            .eventId(eventId)
            .eventName("Comparison Test Event")
            .totalQuantity(totalQuantity)
            .availableQuantity(totalQuantity)
            .status(TicketStatus.AVAILABLE)
            .build();
        ticketRepository.save(ticket);
    }

    @Transactional
    public void resetTestTicket(String eventId) {
        ticketRepository.findByEventId(eventId).ifPresent(ticket -> {
            ticket.setAvailableQuantity(ticket.getTotalQuantity());
            ticket.setBookedQuantity(0);
            ticket.setStatus(TicketStatus.AVAILABLE);
            ticket.setBookingReference(null);
            ticket.setBookedBy(null);
            ticket.setBookedAt(null);
            ticket.setIdempotencyKey(null);
            ticket.setVersion(0L);
            ticketRepository.save(ticket);
        });
    }
}