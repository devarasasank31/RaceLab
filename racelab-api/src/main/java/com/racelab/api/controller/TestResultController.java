package com.racelab.api.controller;

import com.racelab.api.dto.TestResultResponse;
import com.racelab.api.mapper.TestResultMapper;
import com.racelab.core.domain.TestExecution;
import com.racelab.core.domain.TestResult;
import com.racelab.core.exception.ExecutionNotFoundException;
import com.racelab.core.repository.TestExecutionRepository;
import com.racelab.core.repository.TestResultRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for retrieving test results.
 */
@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
@Tag(name = "Test Results", description = "Retrieve test execution results and metrics")
public class TestResultController {

    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;
    private final TestResultMapper resultMapper;

    @GetMapping("/execution/{executionId}")
    @Operation(summary = "Get test results for an execution")
    public ResponseEntity<TestResultResponse> getResultsForExecution(
            @Parameter(description = "Execution ID") @PathVariable Long executionId) {
        TestExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new ExecutionNotFoundException(executionId));

        Optional<TestResult> result = resultRepository.findByExecution(execution);
        return result.map(r -> ResponseEntity.ok(resultMapper.toResponse(r)))
            .orElse(ResponseEntity.notFound().build());
    }
}