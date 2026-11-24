package com.racelab.api.controller;

import com.racelab.core.domain.ConcurrencyStrategyType;
import com.racelab.engine.demo.RaceConditionDemoService;
import com.racelab.engine.dto.RaceConditionDemoRequest;
import com.racelab.engine.dto.RaceConditionDemoResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for race condition demonstrations.
 * Runs ticket booking scenarios to show race conditions and how
 * different concurrency strategies prevent them.
 */
@RestController
@RequestMapping("/api/demos/race-condition")
@RequiredArgsConstructor
@Tag(name = "Race Condition Demos", description = "Demonstrate race conditions and concurrency strategy effectiveness")
public class RaceConditionDemoController {

    private final RaceConditionDemoService demoService;

    @PostMapping("/run")
    @Operation(summary = "Run race condition demonstration")
    public ResponseEntity<RaceConditionDemoResult> runDemo(@Valid @RequestBody RaceConditionDemoRequest request) {
        RaceConditionDemoResult result = demoService.runDemo(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/run-vulnerable")
    @Operation(summary = "Run only the vulnerable (no protection) test")
    public ResponseEntity<RaceConditionDemoResult.VulnerableResult> runVulnerable(
            @RequestParam String eventId,
            @RequestParam(defaultValue = "1") int totalTickets,
            @RequestParam(defaultValue = "1000") int concurrentRequests,
            @RequestParam(defaultValue = "1") int quantityPerRequest) {

        RaceConditionDemoRequest request = RaceConditionDemoRequest.builder()
            .eventId(eventId)
            .totalTickets(totalTickets)
            .concurrentRequests(concurrentRequests)
            .quantityPerRequest(quantityPerRequest)
            .runVulnerable(true)
            .build();

        RaceConditionDemoResult result = demoService.runDemo(request);
        return ResponseEntity.ok(result.getVulnerableResult());
    }

    @PostMapping("/run-strategy")
    @Operation(summary = "Run a specific concurrency strategy test")
    public ResponseEntity<RaceConditionDemoResult.StrategyResult> runStrategy(
            @RequestParam String eventId,
            @RequestParam ConcurrencyStrategyType strategy,
            @RequestParam(defaultValue = "1") int totalTickets,
            @RequestParam(defaultValue = "1000") int concurrentRequests,
            @RequestParam(defaultValue = "1") int quantityPerRequest) {

        RaceConditionDemoRequest request = RaceConditionDemoRequest.builder()
            .eventId(eventId)
            .totalTickets(totalTickets)
            .concurrentRequests(concurrentRequests)
            .quantityPerRequest(quantityPerRequest)
            .strategies(List.of(strategy))
            .build();

        RaceConditionDemoResult result = demoService.runDemo(request);
        RaceConditionDemoResult.StrategyResult strategyResult = 
            result.getStrategyResults().get(strategy);

        return strategyResult != null 
            ? ResponseEntity.ok(strategyResult)
            : ResponseEntity.notFound().build();
    }

    @GetMapping("/strategies")
    @Operation(summary = "List available concurrency strategies")
    public ResponseEntity<List<String>> getStrategies() {
        return ResponseEntity.ok(
            Arrays.stream(ConcurrencyStrategyType.values())
                .filter(s -> s != ConcurrencyStrategyType.NONE
                    && s != ConcurrencyStrategyType.REDIS_DISTRIBUTED_LOCK)
                .map(Enum::name)
                .collect(Collectors.toList())
        );
    }

    @PostMapping("/compare-all")
    @Operation(summary = "Run comparison of all strategies")
    public ResponseEntity<RaceConditionDemoResult> compareAll(
            @RequestParam String eventId,
            @RequestParam(defaultValue = "1") int totalTickets,
            @RequestParam(defaultValue = "1000") int concurrentRequests,
            @RequestParam(defaultValue = "1") int quantityPerRequest) {

        RaceConditionDemoRequest request = RaceConditionDemoRequest.builder()
            .eventId(eventId)
            .totalTickets(totalTickets)
            .concurrentRequests(concurrentRequests)
            .quantityPerRequest(quantityPerRequest)
            .strategies(Arrays.stream(ConcurrencyStrategyType.values())
                .filter(s -> s != ConcurrencyStrategyType.NONE
                    && s != ConcurrencyStrategyType.REDIS_DISTRIBUTED_LOCK)
                .toList())
            .build();

        RaceConditionDemoResult result = demoService.runDemo(request);
        return ResponseEntity.ok(result);
    }
}