package com.racelab.engine.runner;

import com.racelab.core.domain.HttpMethod;
import com.racelab.core.domain.RequestResult;
import com.racelab.core.domain.TestExecution;
import com.racelab.core.domain.TestScenario;
import com.racelab.core.exception.TestExecutionException;
import com.racelab.core.util.CancellationToken;
import com.racelab.core.util.ConcurrencyUtils;
import com.racelab.engine.aggregator.ResultAggregator;
import com.racelab.engine.http.HttpClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Main concurrency test runner.
 * Executes configurable concurrent HTTP requests with controlled ramp-up and result aggregation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConcurrencyTestRunner {

    private final HttpClientService httpClientService;
    private final ResultAggregator resultAggregator;
    private final ConcurrencyUtils concurrencyUtils;

    /**
     * Run a test scenario and return the execution with results.
     */
    public TestExecution runTest(TestScenario scenario, CancellationToken cancellationToken) {
        TestExecution execution = createExecution(scenario);
        ResultAggregator.AggregatedResults results = resultAggregator.createAggregatedResults();

        execution.setStatus(com.racelab.core.domain.TestStatus.RUNNING);
        execution.setStartedAt(Instant.now());

        try {
            if (scenario.isDurationBased()) {
                runDurationBased(scenario, execution, results, cancellationToken);
            } else {
                runRequestCountBased(scenario, execution, results, cancellationToken);
            }

            if (cancellationToken.isCancelled()) {
                execution.setStatus(com.racelab.core.domain.TestStatus.CANCELLED);
                execution.setCancelledAt(Instant.now());
            } else {
                execution.setStatus(com.racelab.core.domain.TestStatus.COMPLETED);
                execution.setCompletedAt(Instant.now());
            }

        } catch (Exception e) {
            log.error("Test execution failed: {}", e.getMessage(), e);
            execution.setStatus(com.racelab.core.domain.TestStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(Instant.now());
            throw new TestExecutionException("Test execution failed: " + e.getMessage(), e);
        } finally {
            resultAggregator.updateExecutionCounts(execution, results);
        }

        return execution;
    }

    /**
     * Run a test for a specified duration.
     */
    private void runDurationBased(TestScenario scenario, TestExecution execution,
                                  ResultAggregator.AggregatedResults results,
                                  CancellationToken cancellationToken) {

        int concurrentUsers = scenario.getConcurrentUsers();
        int durationSeconds = scenario.getDurationSeconds();
        int rampUpSeconds = scenario.getRampUpSeconds() != null ? scenario.getRampUpSeconds() : 0;

        ExecutorService executor = createExecutor(scenario, concurrentUsers);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        AtomicLong requestCounter = new AtomicLong(0);
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(durationSeconds);

        // Submit initial batch of tasks
        for (int i = 0; i < concurrentUsers; i++) {
            if (cancellationToken.isCancelled()) break;
            submitRequestTask(completionService, scenario, execution, results, requestCounter, cancellationToken);
        }

        // Ramp-up: gradually increase concurrent users
        if (rampUpSeconds > 0) {
            runWithRampUp(scenario, execution, results, completionService, requestCounter,
                concurrentUsers, rampUpSeconds, endTime, cancellationToken);
        } else {
            runSteadyState(completionService, requestCounter, endTime, cancellationToken);
        }

        // Wait for remaining tasks
        waitForCompletion(completionService, requestCounter.get(), cancellationToken);

        concurrencyUtils.shutdownGracefully(executor, 30);
    }

    /**
     * Run a test for a specified number of requests.
     */
    private void runRequestCountBased(TestScenario scenario, TestExecution execution,
                                      ResultAggregator.AggregatedResults results,
                                      CancellationToken cancellationToken) {

        int concurrentUsers = scenario.getConcurrentUsers();
        long totalRequests = scenario.getTotalRequests();
        int rampUpSeconds = scenario.getRampUpSeconds() != null ? scenario.getRampUpSeconds() : 0;

        ExecutorService executor = createExecutor(scenario, concurrentUsers);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        AtomicLong requestCounter = new AtomicLong(0);
        Instant startTime = Instant.now();

        // Submit initial batch
        int initialBatch = Math.min(concurrentUsers, (int) totalRequests);
        for (int i = 0; i < initialBatch; i++) {
            if (cancellationToken.isCancelled()) break;
            submitRequestTask(completionService, scenario, execution, results, requestCounter, cancellationToken);
        }

        if (rampUpSeconds > 0) {
            runWithRampUpRequestBased(scenario, execution, results, completionService,
                requestCounter, totalRequests, concurrentUsers, rampUpSeconds, cancellationToken);
        } else {
            runSteadyStateRequestBased(completionService, requestCounter, totalRequests, cancellationToken);
        }

        waitForCompletion(completionService, requestCounter.get(), cancellationToken);

        concurrencyUtils.shutdownGracefully(executor, 30);
    }

    private void runWithRampUp(TestScenario scenario, TestExecution execution,
                               ResultAggregator.AggregatedResults results,
                               CompletionService<Void> completionService,
                               AtomicLong requestCounter, int targetConcurrency,
                               int rampUpSeconds, Instant endTime,
                               CancellationToken cancellationToken) {

        long rampUpStart = System.currentTimeMillis();
        long rampUpEnd = rampUpStart + (rampUpSeconds * 1000L);
        int currentConcurrency = 1;

        while (System.currentTimeMillis() < rampUpEnd && !cancellationToken.isCancelled()) {
            long elapsed = System.currentTimeMillis() - rampUpStart;
            double progress = (double) elapsed / (rampUpSeconds * 1000.0);
            int desiredConcurrency = Math.max(1, (int) (targetConcurrency * progress));

            if (desiredConcurrency > currentConcurrency) {
                int toAdd = desiredConcurrency - currentConcurrency;
                for (int i = 0; i < toAdd; i++) {
                    submitRequestTask(completionService, scenario, execution, results, requestCounter, cancellationToken);
                }
                currentConcurrency = desiredConcurrency;
            }

            // Process completed tasks and submit new ones
            processCompletedAndSubmit(completionService, requestCounter, scenario, execution, results, cancellationToken);

            concurrencyUtils.sleepUninterruptibly(100);
        }

        // Steady state
        runSteadyState(completionService, requestCounter, endTime, cancellationToken);
    }

    private void runWithRampUpRequestBased(TestScenario scenario, TestExecution execution,
                                           ResultAggregator.AggregatedResults results,
                                           CompletionService<Void> completionService,
                                           AtomicLong requestCounter, long totalRequests,
                                           int targetConcurrency, int rampUpSeconds,
                                           CancellationToken cancellationToken) {

        long rampUpStart = System.currentTimeMillis();
        long rampUpEnd = rampUpStart + (rampUpSeconds * 1000L);
        int currentConcurrency = 1;

        while (requestCounter.get() < totalRequests && System.currentTimeMillis() < rampUpEnd
            && !cancellationToken.isCancelled()) {

            long elapsed = System.currentTimeMillis() - rampUpStart;
            double progress = (double) elapsed / (rampUpSeconds * 1000.0);
            int desiredConcurrency = Math.max(1, (int) (targetConcurrency * progress));

            if (desiredConcurrency > currentConcurrency) {
                int toAdd = desiredConcurrency - currentConcurrency;
                for (int i = 0; i < toAdd && requestCounter.get() < totalRequests; i++) {
                    submitRequestTask(completionService, scenario, execution, results, requestCounter, cancellationToken);
                }
                currentConcurrency = desiredConcurrency;
            }

            processCompletedAndSubmit(completionService, requestCounter, scenario, execution, results, cancellationToken);

            concurrencyUtils.sleepUninterruptibly(100);
        }

        runSteadyStateRequestBased(completionService, requestCounter, totalRequests, cancellationToken);
    }

    private void runSteadyState(CompletionService<Void> completionService,
                                AtomicLong requestCounter, Instant endTime,
                                CancellationToken cancellationToken) {

        while (Instant.now().isBefore(endTime) && !cancellationToken.isCancelled()) {
            processCompletedAndSubmit(completionService, requestCounter, null, null, null, cancellationToken);
            concurrencyUtils.sleepUninterruptibly(50);
        }
    }

    private void runSteadyStateRequestBased(CompletionService<Void> completionService,
                                            AtomicLong requestCounter, long totalRequests,
                                            CancellationToken cancellationToken) {

        while (requestCounter.get() < totalRequests && !cancellationToken.isCancelled()) {
            processCompletedAndSubmit(completionService, requestCounter, null, null, null, cancellationToken);
            concurrencyUtils.sleepUninterruptibly(50);
        }
    }

    private void processCompletedAndSubmit(CompletionService<Void> completionService,
                                           AtomicLong requestCounter,
                                           TestScenario scenario, TestExecution execution,
                                           ResultAggregator.AggregatedResults results,
                                           CancellationToken cancellationToken) {

        try {
            Future<Void> completed = completionService.poll(10, TimeUnit.MILLISECONDS);
            if (completed != null) {
                completed.get(); // Propagate exceptions
                // Submit replacement task if still running
                if (scenario != null && execution != null && results != null && !cancellationToken.isCancelled()) {
                    submitRequestTask(completionService, scenario, execution, results, requestCounter, cancellationToken);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancellationToken.cancel("Interrupted");
        } catch (ExecutionException e) {
            log.warn("Task failed: {}", e.getCause().getMessage());
        }
    }

    private void submitRequestTask(CompletionService<Void> completionService,
                                   TestScenario scenario, TestExecution execution,
                                   ResultAggregator.AggregatedResults results,
                                   AtomicLong requestCounter,
                                   CancellationToken cancellationToken) {

        long requestNumber = requestCounter.incrementAndGet();

        completionService.submit(() -> {
            cancellationToken.throwIfCancelled();

            HttpClientService.HttpRequest request = buildHttpRequest(scenario, requestNumber);
            HttpClientService.HttpResponse response = httpClientService.execute(request);

            recordResult(execution, results, requestNumber, response);
            return null;
        });
    }

    private HttpClientService.HttpRequest buildHttpRequest(TestScenario scenario, long requestNumber) {
        String url = scenario.getTargetEndpoint();

        // Generate idempotency key if strategy requires it
        String idempotencyKey = null;
        if (scenario.getConcurrencyStrategy().requiresIdempotencyKey()) {
            idempotencyKey = String.format("%s-%d-%d", scenario.getName(), requestNumber, System.nanoTime());
        }

        return new HttpClientService.HttpRequest(
            scenario.getHttpMethod(),
            url,
            scenario.getRequestHeaders(),
            scenario.getRequestPayload(),
            Duration.ofSeconds(scenario.getRequestTimeoutSeconds()),
            idempotencyKey
        );
    }

    private void recordResult(TestExecution execution, ResultAggregator.AggregatedResults results,
                              long requestNumber, HttpClientService.HttpResponse response) {

        // Create RequestResult entity for persistence
        RequestResult requestResult = RequestResult.builder()
            .execution(execution)
            .requestNumber(requestNumber)
            .threadName(Thread.currentThread().getName())
            .virtualThread(Thread.currentThread().isVirtual())
            .timestamp(java.time.Instant.now())
            .latencyMs(response.getLatencyMs())
            .statusCode(response.getStatusCode())
            .success(response.isSuccess())
            .errorMessage(response.getErrorMessage())
            .responseSizeBytes(response.getResponseSizeBytes())
            .build();

        // Aggregate
        if (response.isSuccess()) {
            results.recordSuccess(response.getLatencyMs(), response.getStatusCode(), response.getResponseSizeBytes());
        } else {
            results.recordFailure(response.getLatencyMs(), response.getStatusCode(), response.getErrorMessage());
        }
    }

    private void waitForCompletion(CompletionService<Void> completionService, long expectedCount,
                                   CancellationToken cancellationToken) {
        long completed = 0;
        while (completed < expectedCount && !cancellationToken.isCancelled()) {
            try {
                Future<Void> future = completionService.poll(1, TimeUnit.SECONDS);
                if (future != null) {
                    future.get();
                    completed++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancellationToken.cancel("Interrupted");
                break;
            } catch (ExecutionException e) {
                log.warn("Task failed during completion: {}", e.getCause().getMessage());
                completed++;
            }
        }
    }

    private ExecutorService createExecutor(TestScenario scenario, int concurrentUsers) {
        if (scenario.getHttpMethod() == HttpMethod.GET) {
            // GET requests are typically I/O-bound - use virtual threads
            return concurrencyUtils.createVirtualThreadExecutor("racelab-http");
        } else {
            // POST/PUT/PATCH may have more CPU work - use bounded platform thread pool
            return concurrencyUtils.createCpuThreadPool(
                4, Math.min(concurrentUsers, 16), 10000, "racelab-http");
        }
    }

    private TestExecution createExecution(TestScenario scenario) {
        return TestExecution.builder()
            .scenario(scenario)
            .status(com.racelab.core.domain.TestStatus.PENDING)
            .configurationSnapshot(snapshotConfiguration(scenario))
            .build();
    }

    private java.util.Map<String, Object> snapshotConfiguration(TestScenario scenario) {
        return java.util.Map.of(
            "concurrentUsers", scenario.getConcurrentUsers(),
            "totalRequests", scenario.getTotalRequests(),
            "durationSeconds", scenario.getDurationSeconds(),
            "rampUpSeconds", scenario.getRampUpSeconds(),
            "targetEndpoint", scenario.getTargetEndpoint(),
            "httpMethod", scenario.getHttpMethod().name(),
            "concurrencyStrategy", scenario.getConcurrencyStrategy().name(),
            "requestTimeoutSeconds", scenario.getRequestTimeoutSeconds(),
            "maxRetries", scenario.getMaxRetries()
        );
    }
}