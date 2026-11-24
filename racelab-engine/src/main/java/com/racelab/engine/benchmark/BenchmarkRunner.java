package com.racelab.engine.benchmark;

import com.racelab.core.booking.BookingService;
import com.racelab.core.booking.BookingResult;
import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.core.domain.Ticket;
import com.racelab.core.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Benchmark runner for comparing concurrency strategies.
 * Runs controlled experiments and collects performance metrics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BenchmarkRunner {

    private final Map<ConcurrencyStrategyType, BookingService> strategyServices;
    private final TicketRepository ticketRepository;

    /**
     * Run a benchmark comparing multiple strategies.
     */
    public BenchmarkResult runBenchmark(BenchmarkConfig config) {
        log.info("Starting benchmark: {} strategies, {} tickets, {} requests, {} iterations",
            config.getStrategies().size(),
            config.getTotalTickets(),
            config.getConcurrentRequests(),
            config.getIterations());

        List<StrategyBenchmarkResult> strategyResults = new ArrayList<>();

        for (ConcurrencyStrategyType strategy : config.getStrategies()) {
            BookingService service = strategyServices.get(strategy);
            if (service == null) {
                log.warn("No service for strategy: {}", strategy);
                continue;
            }

            StrategyBenchmarkResult result = runStrategyBenchmark(service, strategy, config);
            strategyResults.add(result);

            log.info("Strategy {} completed: avg success rate={:.1f}%, avg latency={:.2f}ms",
                strategy, result.getAvgSuccessRate(), result.getAvgLatencyMs());
        }

        return new BenchmarkResult(
            config.getName(),
            Instant.now(),
            config,
            strategyResults
        );
    }

    /**
     * Run benchmark for a single strategy with multiple iterations.
     */
    private StrategyBenchmarkResult runStrategyBenchmark(
            BookingService service,
            ConcurrencyStrategyType strategy,
            BenchmarkConfig config) {

        List<IterationResult> iterations = new ArrayList<>();

        for (int i = 0; i < config.getIterations(); i++) {
            // Create fresh tickets for each iteration
            createTickets(config.getEventId() + "-" + i, config.getTotalTickets());

            IterationResult iteration = runSingleIteration(service, config);
            iterations.add(iteration);

            // Small delay between iterations
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return new StrategyBenchmarkResult(strategy, iterations);
    }

    /**
     * Run a single benchmark iteration.
     */
    private IterationResult runSingleIteration(BookingService service, BenchmarkConfig config) {
        int concurrentRequests = config.getConcurrentRequests();
        int quantityPerRequest = config.getQuantityPerRequest();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < concurrentRequests; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long start = System.nanoTime();
                    BookingResult result = service.book(config.getEventId(), "user-" + userId, quantityPerRequest);
                    long latencyMs = (System.nanoTime() - start) / 1_000_000;
                    latencies.add(latencyMs);

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

        long iterationStart = System.currentTimeMillis();
        startLatch.countDown();
        boolean completed = false;
        try {
            completed = endLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long iterationDurationMs = System.currentTimeMillis() - iterationStart;
        executor.shutdownNow();

        // Calculate metrics
        double successRate = concurrentRequests > 0 ? (double) successCount.get() / concurrentRequests * 100.0 : 0.0;
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double p50 = percentile(latencies, 50);
        double p95 = percentile(latencies, 95);
        double p99 = percentile(latencies, 99);

        // Check final state
        var ticket = ticketRepository.findByEventId(config.getEventId() + "-" + 0).orElse(null);
        int finalAvailable = ticket != null ? ticket.getAvailableQuantity() : -1;
        int finalBooked = ticket != null ? ticket.getBookedQuantity() : -1;
        boolean consistent = finalBooked <= config.getTotalTickets() && finalAvailable >= 0;

        return new IterationResult(
            completed,
            successCount.get(),
            failureCount.get(),
            successRate,
            avgLatency,
            p50,
            p95,
            p99,
            iterationDurationMs,
            finalAvailable,
            finalBooked,
            consistent
        );
    }

    private double percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0.0;
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    private void createTickets(String eventId, int count) {
        for (int i = 0; i < count; i++) {
            String id = count == 1 ? eventId : eventId + "-" + i;
            Ticket ticket = Ticket.builder()
                .eventId(id)
                .eventName("Benchmark Event")
                .totalQuantity(1)
                .availableQuantity(1)
                .status(com.racelab.core.domain.TicketStatus.AVAILABLE)
                .build();
            ticketRepository.save(ticket);
        }
    }

    // Result classes
    public record BenchmarkResult(
        String name,
        Instant timestamp,
        BenchmarkConfig config,
        List<StrategyBenchmarkResult> results
    ) {}

    public record BenchmarkConfig(
        String name,
        String eventId,
        int totalTickets,
        int concurrentRequests,
        int quantityPerRequest,
        int iterations,
        List<ConcurrencyStrategyType> strategies
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name = "benchmark";
            private String eventId = "bench-event";
            private int totalTickets = 1;
            private int concurrentRequests = 100;
            private int quantityPerRequest = 1;
            private int iterations = 5;
            private List<ConcurrencyStrategyType> strategies = List.of(
                ConcurrencyStrategyType.OPTIMISTIC_LOCKING,
                ConcurrencyStrategyType.PESSIMISTIC_LOCKING,
                ConcurrencyStrategyType.ATOMIC_UPDATE,
                ConcurrencyStrategyType.IDEMPOTENCY_KEY
            );

            public Builder name(String name) { this.name = name; return this; }
            public Builder eventId(String eventId) { this.eventId = eventId; return this; }
            public Builder totalTickets(int totalTickets) { this.totalTickets = totalTickets; return this; }
            public Builder concurrentRequests(int concurrentRequests) { this.concurrentRequests = concurrentRequests; return this; }
            public Builder quantityPerRequest(int qty) { this.quantityPerRequest = qty; return this; }
            public Builder iterations(int iterations) { this.iterations = iterations; return this; }
            public Builder strategies(List<ConcurrencyStrategyType> strategies) { this.strategies = strategies; return this; }
            public BenchmarkConfig build() { return new BenchmarkConfig(name, eventId, totalTickets, concurrentRequests, quantityPerRequest, iterations, strategies); }
        }
    }

    public record StrategyBenchmarkResult(
        ConcurrencyStrategyType strategy,
        List<IterationResult> iterations
    ) {
        public double getAvgSuccessRate() {
            return iterations.stream().mapToDouble(IterationResult::successRate).average().orElse(0.0);
        }
        public double getAvgLatencyMs() {
            return iterations.stream().mapToDouble(IterationResult::avgLatencyMs).average().orElse(0.0);
        }
        public double getAvgP95LatencyMs() {
            return iterations.stream().mapToDouble(IterationResult::p95LatencyMs).average().orElse(0.0);
        }
        public int getTotalSuccesses() {
            return iterations.stream().mapToInt(IterationResult::successes).sum();
        }
        public int getTotalFailures() {
            return iterations.stream().mapToInt(IterationResult::failures).sum();
        }
    }

    public record IterationResult(
        boolean completed,
        int successes,
        int failures,
        double successRate,
        double avgLatencyMs,
        double p50LatencyMs,
        double p95LatencyMs,
        double p99LatencyMs,
        long durationMs,
        int finalAvailable,
        int finalBooked,
        boolean consistent
    ) {}
}