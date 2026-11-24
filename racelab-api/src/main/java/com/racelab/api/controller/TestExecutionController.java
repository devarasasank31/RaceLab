package com.racelab.api.controller;

import com.racelab.api.dto.PageResponse;
import com.racelab.api.dto.TestExecutionRequest;
import com.racelab.api.dto.TestExecutionResponse;
import com.racelab.api.mapper.TestExecutionMapper;
import com.racelab.engine.service.TestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for managing test executions.
 */
@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@Tag(name = "Test Executions", description = "Execute and manage concurrency tests")
public class TestExecutionController {

    private final TestExecutionService executionService;
    private final TestExecutionMapper executionMapper;

    @PostMapping("/{scenarioId}/start")
    @Operation(summary = "Start a test execution asynchronously")
    public ResponseEntity<TestExecutionResponse> startExecution(
            @Parameter(description = "Scenario ID") @PathVariable Long scenarioId,
            @RequestBody(required = false) TestExecutionRequest request) {
        CompletableFuture<com.racelab.core.domain.TestExecution> future = executionService.startExecution(scenarioId);
        // Return immediately with PENDING status
        com.racelab.core.domain.TestExecution execution = future.join(); // For now, wait for completion
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(executionMapper.toResponse(execution));
    }

    @PostMapping("/{scenarioId}/start-sync")
    @Operation(summary = "Start a test execution synchronously (for testing)")
    public ResponseEntity<TestExecutionResponse> startExecutionSync(
            @Parameter(description = "Scenario ID") @PathVariable Long scenarioId,
            @RequestBody(required = false) TestExecutionRequest request) {
        com.racelab.core.domain.TestExecution execution = executionService.startExecutionSync(scenarioId);
        return ResponseEntity.ok(executionMapper.toResponse(execution));
    }

    @PostMapping("/{executionId}/cancel")
    @Operation(summary = "Cancel a running test execution")
    public ResponseEntity<Void> cancelExecution(
            @Parameter(description = "Execution ID") @PathVariable Long executionId) {
        executionService.cancelExecution(executionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{executionId}")
    @Operation(summary = "Get a test execution by ID")
    public ResponseEntity<TestExecutionResponse> getExecution(
            @Parameter(description = "Execution ID") @PathVariable Long executionId) {
        com.racelab.core.domain.TestExecution execution = executionService.getExecution(executionId);
        return ResponseEntity.ok(executionMapper.toResponse(execution));
    }

    @GetMapping("/scenario/{scenarioId}")
    @Operation(summary = "Get all executions for a scenario")
    public ResponseEntity<List<TestExecutionResponse>> getExecutionsForScenario(
            @Parameter(description = "Scenario ID") @PathVariable Long scenarioId) {
        List<com.racelab.core.domain.TestExecution> executions = executionService.getExecutionsForScenario(scenarioId);
        List<TestExecutionResponse> response = executions.stream()
            .map(executionMapper::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent executions across all scenarios")
    public ResponseEntity<List<TestExecutionResponse>> getRecentExecutions(
            @Parameter(description = "Maximum number of executions to return") @RequestParam(defaultValue = "20") int limit) {
        List<com.racelab.core.domain.TestExecution> executions = executionService.getRecentExecutions(limit);
        List<TestExecutionResponse> response = executions.stream()
            .map(executionMapper::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/running")
    @Operation(summary = "Get currently running executions")
    public ResponseEntity<List<TestExecutionResponse>> getRunningExecutions() {
        List<com.racelab.core.domain.TestExecution> executions = executionService.getRunningExecutions();
        List<TestExecutionResponse> response = executions.stream()
            .map(executionMapper::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }
}