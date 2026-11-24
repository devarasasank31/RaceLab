package com.racelab.engine.runner;

import com.racelab.core.domain.HttpMethod;
import com.racelab.core.domain.RequestResult;
import com.racelab.core.domain.TestExecution;
import com.racelab.core.domain.TestScenario;
import com.racelab.core.util.CancellationToken;
import com.racelab.engine.aggregator.ResultAggregator;
import com.racelab.engine.http.HttpClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ConcurrencyTestRunner using mocked HTTP client.
 */
@ExtendWith(MockitoExtension.class)
class ConcurrencyTestRunnerTest {

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private ResultAggregator resultAggregator;

    private ConcurrencyTestRunner createRunner() {
        return new ConcurrencyTestRunner(httpClientService, resultAggregator, new com.racelab.core.util.ConcurrencyUtils());
    }

    @Test
    void runTest_RequestCountBased_ShouldExecuteCorrectNumberOfRequests() throws Exception {
        TestScenario scenario = TestScenario.builder()
            .name("test-scenario")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .requestTimeoutSeconds(30)
            .build();

        CancellationToken token = CancellationToken.create();

        // Mock successful responses
        when(httpClientService.execute(any(HttpClientService.HttpRequest.class)))
            .thenReturn(new HttpClientService.HttpResponse(200, "OK", null, 10, true, null, 100));

        ResultAggregator.AggregatedResults mockResults = resultAggregator.createAggregatedResults();
        when(resultAggregator.createAggregatedResults()).thenReturn(mockResults);

        ConcurrencyTestRunner runner = createRunner();
        TestExecution execution = runner.runTest(scenario, token);

        assertThat(execution.getStatus()).isEqualTo(com.racelab.core.domain.TestStatus.COMPLETED);
        assertThat(execution.getExecutedRequests()).isEqualTo(100);
    }

    @Test
    void runTest_DurationBased_ShouldRunForSpecifiedDuration() throws Exception {
        TestScenario scenario = TestScenario.builder()
            .name("duration-test")
            .concurrentUsers(5)
            .durationSeconds(1) // 1 second
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.GET)
            .requestTimeoutSeconds(30)
            .build();

        CancellationToken token = CancellationToken.create();

        when(httpClientService.execute(any(HttpClientService.HttpRequest.class)))
            .thenReturn(new HttpClientService.HttpResponse(200, "OK", null, 5, true, null, 100));

        ResultAggregator.AggregatedResults mockResults = resultAggregator.createAggregatedResults();
        when(resultAggregator.createAggregatedResults()).thenReturn(mockResults);

        ConcurrencyTestRunner runner = createRunner();
        TestExecution execution = runner.runTest(scenario, token);

        assertThat(execution.getStatus()).isEqualTo(com.racelab.core.domain.TestStatus.COMPLETED);
        assertThat(execution.getExecutedRequests()).isGreaterThan(0);
    }

    @Test
    void runTest_WithCancellation_ShouldCancelGracefully() throws Exception {
        TestScenario scenario = TestScenario.builder()
            .name("cancel-test")
            .concurrentUsers(10)
            .totalRequests(10000) // Large number, but we'll cancel early
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .requestTimeoutSeconds(30)
            .build();

        CancellationToken token = CancellationToken.create();

        when(httpClientService.execute(any(HttpClientService.HttpRequest.class)))
            .thenAnswer(invocation -> {
                Thread.sleep(10); // Simulate some work
                return new HttpClientService.HttpResponse(200, "OK", null, 10, true, null, 100);
            });

        ResultAggregator.AggregatedResults mockResults = resultAggregator.createAggregatedResults();
        when(resultAggregator.createAggregatedResults()).thenReturn(mockResults);

        ConcurrencyTestRunner runner = createRunner();

        // Start test in background and cancel quickly
        CompletableFuture<TestExecution> future = CompletableFuture.supplyAsync(() ->
            runner.runTest(scenario, token)
        );

        Thread.sleep(50); // Let some requests start
        token.cancel("Test cancellation");

        TestExecution execution = future.join();

        assertThat(execution.getStatus()).isEqualTo(com.racelab.core.domain.TestStatus.CANCELLED);
    }

    @Test
    void runTest_HttpErrors_ShouldTrackFailures() throws Exception {
        TestScenario scenario = TestScenario.builder()
            .name("error-test")
            .concurrentUsers(5)
            .totalRequests(20)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .requestTimeoutSeconds(30)
            .build();

        CancellationToken token = CancellationToken.create();

        when(httpClientService.execute(any(HttpClientService.HttpRequest.class)))
            .thenReturn(new HttpClientService.HttpResponse(500, "Error", null, 10, false, "Internal Server Error", 0));

        ResultAggregator.AggregatedResults mockResults = resultAggregator.createAggregatedResults();
        when(resultAggregator.createAggregatedResults()).thenReturn(mockResults);

        ConcurrencyTestRunner runner = createRunner();
        TestExecution execution = runner.runTest(scenario, token);

        assertThat(execution.getStatus()).isEqualTo(com.racelab.core.domain.TestStatus.COMPLETED);
        assertThat(execution.getFailedRequests()).isEqualTo(20);
        assertThat(execution.getSuccessfulRequests()).isZero();
    }
}