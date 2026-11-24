package com.racelab.engine.service;

import com.racelab.core.domain.TestExecution;
import com.racelab.core.domain.TestScenario;
import com.racelab.core.exception.ExecutionNotFoundException;
import com.racelab.core.exception.ScenarioNotFoundException;
import com.racelab.core.exception.TestExecutionException;
import com.racelab.core.repository.TestExecutionRepository;
import com.racelab.core.repository.TestResultRepository;
import com.racelab.core.repository.TestScenarioRepository;
import com.racelab.core.util.CancellationToken;
import com.racelab.engine.runner.ConcurrencyTestRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing test executions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestExecutionService {

    private final TestScenarioRepository scenarioRepository;
    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;
    private final ConcurrencyTestRunner testRunner;

    // Track running executions for cancellation
    private final java.util.concurrent.ConcurrentHashMap<Long, CancellationToken> runningExecutions = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Start a test execution asynchronously.
     */
    @Transactional
    public CompletableFuture<TestExecution> startExecution(Long scenarioId) {
        TestScenario scenario = scenarioRepository.findById(scenarioId)
            .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        scenario.validateConfiguration();

        CancellationToken cancellationToken = CancellationToken.create();
        TestExecution execution = executionRepository.save(
            TestExecution.builder()
                .scenario(scenario)
                .status(com.racelab.core.domain.TestStatus.PENDING)
                .build()
        );

        runningExecutions.put(execution.getId(), cancellationToken);

        return CompletableFuture.supplyAsync(() -> {
            try {
                TestExecution completed = testRunner.runTest(scenario, cancellationToken);
                return executionRepository.save(completed);
            } finally {
                runningExecutions.remove(execution.getId());
            }
        });
    }

    /**
     * Start a test execution synchronously (for testing).
     */
    @Transactional
    public TestExecution startExecutionSync(Long scenarioId) {
        TestScenario scenario = scenarioRepository.findById(scenarioId)
            .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        scenario.validateConfiguration();

        CancellationToken cancellationToken = CancellationToken.create();
        TestExecution execution = executionRepository.save(
            TestExecution.builder()
                .scenario(scenario)
                .status(com.racelab.core.domain.TestStatus.PENDING)
                .build()
        );

        runningExecutions.put(execution.getId(), cancellationToken);

        try {
            TestExecution completed = testRunner.runTest(scenario, cancellationToken);
            return executionRepository.save(completed);
        } finally {
            runningExecutions.remove(execution.getId());
        }
    }

    /**
     * Cancel a running execution.
     */
    @Transactional
    public void cancelExecution(Long executionId) {
        CancellationToken token = runningExecutions.get(executionId);
        if (token != null) {
            token.cancel("Cancelled by user");
            log.info("Cancellation requested for execution {}", executionId);
        } else {
            // Check if execution exists and is in a cancellable state
            TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ExecutionNotFoundException(executionId));

            if (execution.isTerminal()) {
                throw new TestExecutionException("Cannot cancel execution in terminal state: " + execution.getStatus());
            }

            // Mark as cancelled in database
            execution.setStatus(com.racelab.core.domain.TestStatus.CANCELLED);
            execution.setCancelledAt(java.time.Instant.now());
            executionRepository.save(execution);
        }
    }

    /**
     * Get an execution by ID.
     */
    @Transactional(readOnly = true)
    public TestExecution getExecution(Long executionId) {
        return executionRepository.findById(executionId)
            .orElseThrow(() -> new ExecutionNotFoundException(executionId));
    }

    /**
     * Get all executions for a scenario.
     */
    @Transactional(readOnly = true)
    public List<TestExecution> getExecutionsForScenario(Long scenarioId) {
        TestScenario scenario = scenarioRepository.findById(scenarioId)
            .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));
        return executionRepository.findByScenarioOrderByCreatedAtDesc(scenario);
    }

    /**
     * Get recent executions across all scenarios.
     */
    @Transactional(readOnly = true)
    public List<TestExecution> getRecentExecutions(int limit) {
        return executionRepository.findAll().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(limit)
            .toList();
    }

    /**
     * Get running executions.
     */
    @Transactional(readOnly = true)
    public List<TestExecution> getRunningExecutions() {
        return executionRepository.findByStatus(com.racelab.core.domain.TestStatus.RUNNING);
    }
}