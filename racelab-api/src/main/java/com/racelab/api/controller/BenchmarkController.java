package com.racelab.api.controller;

import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.engine.benchmark.BenchmarkRunner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for running benchmarks and comparing concurrency strategies.
 */
@RestController
@RequestMapping("/api/benchmarks")
@RequiredArgsConstructor
@Tag(name = "Benchmarks", description = "Run performance benchmarks comparing concurrency strategies")
public class BenchmarkController {

    private final BenchmarkRunner benchmarkRunner;

    @PostMapping("/run")
    @Operation(summary = "Run a benchmark comparing concurrency strategies")
    public ResponseEntity<BenchmarkRunner.BenchmarkResult> runBenchmark(
            @Valid @RequestBody BenchmarkRunner.BenchmarkConfig config) {
        BenchmarkRunner.BenchmarkResult result = benchmarkRunner.runBenchmark(config);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/run-single")
    @Operation(summary = "Run a quick single-iteration benchmark")
    public ResponseEntity<BenchmarkRunner.BenchmarkResult> runQuickBenchmark(
            @RequestParam(defaultValue = "quick-benchmark") String name,
            @RequestParam(defaultValue = "bench-event") String eventId,
            @RequestParam(defaultValue = "1") int totalTickets,
            @RequestParam(defaultValue = "100") int concurrentRequests,
            @RequestParam(defaultValue = "1") int quantityPerRequest,
            @RequestParam(required = false) List<ConcurrencyStrategyType> strategies) {

        BenchmarkRunner.BenchmarkConfig config = BenchmarkRunner.BenchmarkConfig.builder()
            .name(name)
            .eventId(eventId)
            .totalTickets(totalTickets)
            .concurrentRequests(concurrentRequests)
            .quantityPerRequest(quantityPerRequest)
            .iterations(1)
            .strategies(strategies != null ? strategies : List.of(
                ConcurrencyStrategyType.OPTIMISTIC_LOCKING,
                ConcurrencyStrategyType.PESSIMISTIC_LOCKING,
                ConcurrencyStrategyType.ATOMIC_UPDATE,
                ConcurrencyStrategyType.IDEMPOTENCY_KEY
            ))
            .build();

        BenchmarkRunner.BenchmarkResult result = benchmarkRunner.runBenchmark(config);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/strategies")
    @Operation(summary = "List available strategies for benchmarking")
    public ResponseEntity<List<String>> getStrategies() {
        return ResponseEntity.ok(
            ConcurrencyStrategyType.values()
                .stream()
                .filter(s -> s != ConcurrencyStrategyType.NONE)
                .map(Enum::name)
                .toList()
        );
    }
}