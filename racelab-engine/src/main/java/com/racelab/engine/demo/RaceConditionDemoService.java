package com.racelab.engine.demo;

import com.racelab.core.booking.*;
import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.core.domain.Ticket;
import com.racelab.core.domain.TicketStatus;
import com.racelab.core.repository.TicketRepository;
import com.racelab.engine.dto.RaceConditionDemoRequest;
import com.racelab.engine.dto.RaceConditionDemoResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for running race condition demonstrations.
 * Executes vulnerable and fixed implementations to show race conditions
 * and how different concurrency strategies prevent them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaceConditionDemoService {

    private final Map<ConcurrencyStrategyType, BookingService> strategyServices;
    private final VulnerableBookingService vulnerableBookingService;
    private final TicketRepository ticketRepository;

    /**
     * Run a complete race condition demonstration.
     */
    public RaceConditionDemoResult runDemo(RaceConditionDemoRequest request) {
        String eventId = request.getEventId();
        int totalTickets = request.getTotalTickets();
        int concurrentRequests = request.getConcurrentRequests();
        int quantityPerRequest = request.getQuantityPerRequest();

        log.info("Starting race condition demo: event={}, tickets={}, concurrent={}",
            eventId, totalTickets, concurrentRequests);

        // Create test ticket
        createTestTicket(eventId, totalTickets);

        // 1. Run VULNERABLE implementation (no protection)
        RaceConditionDemoResult.VulnerableResult vulnerableResult = runVulnerableTest(
            eventId, totalTickets, concurrentRequests, quantityPerRequest
        );

        // 2. Run FIXED implementations (strategies)
        List<ConcurrencyStrategyType> strategies = request.getStrategies();
        if (strategies == null || strategies.isEmpty()) {
            strategies = Arrays.stream(ConcurrencyStrategyType.values())
                .filter(s -> s != ConcurrencyStrategyType.NONE
                    && s != ConcurrencyStrategyType.REDIS_DISTRIBUTED_LOCK)
                .toList();
        }

        Map<ConcurrencyStrategyType, RaceConditionDemoResult.StrategyResult> strategyResults = new LinkedHashMap<>();
        for (ConcurrencyStrategyType strategyType : strategies) {
            BookingService service = strategyServices.get(strategyType);
            if (service == null) {
                log.warn("No service for strategy: {}", strategyType);
                continue;
            }

            // Reset ticket for each strategy test
            resetTestTicket(eventId, totalTickets);

            RaceConditionDemoResult.StrategyResult result = runStrategyTest(
                service, eventId, totalTickets, concurrentRequests, quantityPerRequest
            );
            strategyResults.put(strategyType, result);
        }

        // 3. Build comparison summary
        RaceConditionDemoResult.ComparisonSummary summary = buildSummary(
            vulnerableResult, strategyResults
        );

        // 4. Reset ticket for next run
        resetTestTicket(eventId, totalTickets);

        return RaceConditionDemoResult.builder()
            .eventId(eventId)
            .totalTickets(totalTickets)
            .concurrentRequests(concurrentRequests)
            .timestamp(Instant.now())
            .vulnerableResult(vulnerableResult)
            .strategyResults(strategyResults)
            .summary(summary)
            .build();
    }

    /**
     * Run the vulnerable (no protection) test.
     */
    private RaceConditionDemoResult.VulnerableResult runVulnerableTest(
            String eventId, int totalTickets, int concurrentRequests, int quantityPerRequest) {

        log.info("Running vulnerable test for {}", eventId);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentRequests; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    BookingResult result = vulnerableBookingService.book(eventId, "user-" + userId, quantityPerRequest);
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
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

        // Check final state
        Ticket finalTicket = ticketRepository.findByEventId(eventId).orElse(null);
        int finalAvailable = finalTicket != null ? finalTicket.getAvailableQuantity() : -1;
        int finalBooked = finalTicket != null ? finalTicket.getBookedQuantity() : -1;
        boolean oversold = finalBooked > totalTickets || finalAvailable < 0;

        return RaceConditionDemoResult.VulnerableResult.builder()
            .successfulBookings(successCount.get())
            .failedBookings(failureCount.get())
            .finalAvailableQuantity(finalAvailable)
            .finalBookedQuantity(finalBooked)
            .oversold(oversold)
            .description(oversold
                ? String.format("RACE CONDITION: %d successful bookings for %d ticket(s) - OVERSOLD!", 
                    successCount.get(), totalTickets)
                : String.format("No race condition detected: %d successful bookings", successCount.get()))
            .build();
    }

    /**
     * Run a fixed strategy test.
     */
    private RaceConditionDemoResult.StrategyResult runStrategyTest(
            BookingService service,
            String eventId,
            int totalTickets,
            int concurrentRequests,
            int quantityPerRequest) {

        log.info("Running {} test for {}", service.getStrategyType(), eventId);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentRequests; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    BookingResult result = service.book(eventId, "user-" + userId, quantityPerRequest);
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
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

        // Check final state
        Ticket finalTicket = ticketRepository.findByEventId(eventId).orElse(null);
        int finalAvailable = finalTicket != null ? finalTicket.getAvailableQuantity() : -1;
        int finalBooked = finalTicket != null ? finalTicket.getBookedQuantity() : -1;
        boolean consistent = finalBooked <= totalTickets && finalAvailable >= 0;
        boolean oversold = finalBooked > totalTickets || finalAvailable < 0;

        return RaceConditionDemoResult.StrategyResult.builder()
            .strategyType(service.getStrategyType())
            .successfulBookings(successCount.get())
            .failedBookings(failureCount.get())
            .finalAvailableQuantity(finalAvailable)
            .finalBookedQuantity(finalBooked)
            .consistent(consistent)
            .oversold(oversold)
            .description(buildStrategyDescription(service.getStrategyType(), successCount.get(), totalTickets, consistent))
            .successRate(concurrentRequests > 0 ? (double) successCount.get() / concurrentRequests * 100.0 : 0.0)
            .build();
    }

    private String buildStrategyDescription(ConcurrencyStrategyType strategyType, int successes, int totalTickets, boolean consistent) {
        return switch (strategyType) {
            case OPTIMISTIC_LOCKING -> String.format("Optimistic Locking: %d successes, %d retries - %s",
                successes, Math.max(0, successes - totalTickets), consistent ? "CONSISTENT" : "INCONSISTENT");
            case PESSIMISTIC_LOCKING -> String.format("Pessimistic Locking: %d successes - %s",
                successes, consistent ? "CONSISTENT" : "INCONSISTENT");
            case ATOMIC_UPDATE -> String.format("Atomic Update: %d successes - %s",
                successes, consistent ? "CONSISTENT" : "INCONSISTENT");
            case IDEMPOTENCY_KEY -> String.format("Idempotency Key: %d successes - %s",
                successes, consistent ? "CONSISTENT" : "INCONSISTENT");
            default -> String.format("%s: %d successes - %s", strategyType, successes, consistent ? "CONSISTENT" : "INCONSISTENT");
        };
    }

    private RaceConditionDemoResult.ComparisonSummary buildSummary(
            RaceConditionDemoResult.VulnerableResult vulnerableResult,
            Map<ConcurrencyStrategyType, RaceConditionDemoResult.StrategyResult> strategyResults) {

        Map<ConcurrencyStrategyType, String> strategyDescriptions = strategyResults.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().getDescription()
            ));

        List<String> consistentStrategies = strategyResults.entrySet().stream()
            .filter(e -> e.getValue().isConsistent())
            .map(e -> e.getKey().name())
            .toList();

        List<String> oversoldStrategies = strategyResults.entrySet().stream()
            .filter(e -> e.getValue().isOversold())
            .map(e -> e.getKey().name())
            .toList();

        return RaceConditionDemoResult.ComparisonSummary.builder()
            .vulnerableDescription(vulnerableResult.getDescription())
            .strategyDescriptions(strategyDescriptions)
            .consistentStrategies(consistentStrategies)
            .oversoldStrategies(oversoldStrategies)
            .build();
    }

    @Transactional
    public void createTestTicket(String eventId, int totalTickets) {
        Ticket ticket = Ticket.builder()
            .eventId(eventId)
            .eventName("Race Condition Demo")
            .totalQuantity(totalTickets)
            .availableQuantity(totalTickets)
            .status(TicketStatus.AVAILABLE)
            .build();
        ticketRepository.save(ticket);
    }

    @Transactional
    public void resetTestTicket(String eventId, int totalTickets) {
        ticketRepository.findByEventId(eventId).ifPresent(ticket -> {
            ticket.setAvailableQuantity(totalTickets);
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