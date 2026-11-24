package com.racelab.core.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TestExecution domain entity.
 */
class TestExecutionTest {

    @Test
    void builder_ShouldCreateExecutionWithDefaults() {
        TestScenario scenario = TestScenario.builder()
            .name("test-scenario")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        TestExecution execution = TestExecution.builder()
            .scenario(scenario)
            .build();

        assertThat(execution.getScenario()).isEqualTo(scenario);
        assertThat(execution.getStatus()).isEqualTo(TestStatus.PENDING);
        assertThat(execution.getExecutedRequests()).isZero();
        assertThat(execution.getSuccessfulRequests()).isZero();
        assertThat(execution.getFailedRequests()).isZero();
    }

    @Test
    void getDurationMillis_NotStarted_ShouldReturnZero() {
        TestExecution execution = TestExecution.builder().build();

        assertThat(execution.getDurationMillis()).isZero();
    }

    @Test
    void getDurationMillis_Completed_ShouldReturnDuration() {
        Instant start = Instant.now().minusSeconds(10);
        Instant end = Instant.now();

        TestExecution execution = TestExecution.builder()
            .startedAt(start)
            .completedAt(end)
            .build();

        assertThat(execution.getDurationMillis()).isBetween(9000L, 11000L);
    }

    @Test
    void getDurationMillis_Cancelled_ShouldReturnDurationUntilCancellation() {
        Instant start = Instant.now().minusSeconds(5);
        Instant cancelled = Instant.now();

        TestExecution execution = TestExecution.builder()
            .startedAt(start)
            .cancelledAt(cancelled)
            .build();

        assertThat(execution.getDurationMillis()).isBetween(4000L, 6000L);
    }

    @Test
    void getDurationMillis_Running_ShouldReturnDurationUntilNow() {
        Instant start = Instant.now().minusSeconds(3);

        TestExecution execution = TestExecution.builder()
            .startedAt(start)
            .status(TestStatus.RUNNING)
            .build();

        assertThat(execution.getDurationMillis()).isBetween(2000L, 4000L);
    }

    @Test
    void getSuccessRate_ZeroRequests_ShouldReturnZero() {
        TestExecution execution = TestExecution.builder()
            .executedRequests(0)
            .successfulRequests(0)
            .build();

        assertThat(execution.getSuccessRate()).isZero();
    }

    @Test
    void getSuccessRate_SomeRequests_ShouldCalculateCorrectly() {
        TestExecution execution = TestExecution.builder()
            .executedRequests(100)
            .successfulRequests(95)
            .build();

        assertThat(execution.getSuccessRate()).isEqualTo(95.0);
    }

    @Test
    void getErrorRate_ZeroRequests_ShouldReturnZero() {
        TestExecution execution = TestExecution.builder()
            .executedRequests(0)
            .failedRequests(0)
            .build();

        assertThat(execution.getErrorRate()).isZero();
    }

    @Test
    void getErrorRate_SomeRequests_ShouldCalculateCorrectly() {
        TestExecution execution = TestExecution.builder()
            .executedRequests(100)
            .failedRequests(5)
            .build();

        assertThat(execution.getErrorRate()).isEqualTo(5.0);
    }

    @Test
    void isTerminal_Completed_ShouldReturnTrue() {
        TestExecution execution = TestExecution.builder()
            .status(TestStatus.COMPLETED)
            .build();

        assertThat(execution.isTerminal()).isTrue();
    }

    @Test
    void isTerminal_Cancelled_ShouldReturnTrue() {
        TestExecution execution = TestExecution.builder()
            .status(TestStatus.CANCELLED)
            .build();

        assertThat(execution.isTerminal()).isTrue();
    }

    @Test
    void isTerminal_Failed_ShouldReturnTrue() {
        TestExecution execution = TestExecution.builder()
            .status(TestStatus.FAILED)
            .build();

        assertThat(execution.isTerminal()).isTrue();
    }

    @Test
    void isTerminal_Running_ShouldReturnFalse() {
        TestExecution execution = TestExecution.builder()
            .status(TestStatus.RUNNING)
            .build();

        assertThat(execution.isTerminal()).isFalse();
    }

    @Test
    void isTerminal_Pending_ShouldReturnFalse() {
        TestExecution execution = TestExecution.builder()
            .status(TestStatus.PENDING)
            .build();

        assertThat(execution.isTerminal()).isFalse();
    }
}