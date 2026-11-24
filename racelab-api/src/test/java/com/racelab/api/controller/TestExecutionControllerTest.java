package com.racelab.api.controller;

import com.racelab.api.dto.TestExecutionRequest;
import com.racelab.api.dto.TestExecutionResponse;
import com.racelab.api.mapper.TestExecutionMapper;
import com.racelab.core.domain.TestExecution;
import com.racelab.core.domain.TestScenario;
import com.racelab.core.domain.TestStatus;
import com.racelab.engine.service.TestExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestExecutionController.
 */
@ExtendWith(MockitoExtension.class)
class TestExecutionControllerTest {

    @Mock
    private TestExecutionService executionService;

    @Mock
    private TestExecutionMapper executionMapper;

    private TestExecutionController createController() {
        return new TestExecutionController(executionService, executionMapper);
    }

    @Test
    void startExecution_ValidScenario_ShouldReturnAccepted() {
        TestScenario scenario = TestScenario.builder()
            .id(1L)
            .name("test-scenario")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .build();

        TestExecution execution = TestExecution.builder()
            .id(1L)
            .scenario(scenario)
            .status(TestStatus.COMPLETED)
            .executedRequests(100L)
            .successfulRequests(95L)
            .failedRequests(5L)
            .build();

        TestExecutionResponse response = TestExecutionResponse.builder()
            .id(1L)
            .scenarioId(1L)
            .scenarioName("test-scenario")
            .status(TestStatus.COMPLETED)
            .executedRequests(100L)
            .successfulRequests(95L)
            .failedRequests(5L)
            .build();

        when(executionService.startExecution(1L)).thenReturn(CompletableFuture.completedFuture(execution));
        when(executionMapper.toResponse(execution)).thenReturn(response);

        TestExecutionController controller = createController();
        ResponseEntity<TestExecutionResponse> result = controller.startExecution(1L, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(1L);
        assertThat(result.getBody().getStatus()).isEqualTo(TestStatus.COMPLETED);
    }

    @Test
    void startExecutionSync_ValidScenario_ShouldReturnOk() {
        TestScenario scenario = TestScenario.builder()
            .id(1L)
            .name("test-scenario")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .build();

        TestExecution execution = TestExecution.builder()
            .id(1L)
            .scenario(scenario)
            .status(TestStatus.COMPLETED)
            .executedRequests(100L)
            .successfulRequests(95L)
            .failedRequests(5L)
            .build();

        TestExecutionResponse response = TestExecutionResponse.builder()
            .id(1L)
            .scenarioId(1L)
            .scenarioName("test-scenario")
            .status(TestStatus.COMPLETED)
            .executedRequests(100L)
            .successfulRequests(95L)
            .failedRequests(5L)
            .build();

        when(executionService.startExecutionSync(1L)).thenReturn(execution);
        when(executionMapper.toResponse(execution)).thenReturn(response);

        TestExecutionController controller = createController();
        ResponseEntity<TestExecutionResponse> result = controller.startExecutionSync(1L, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(TestStatus.COMPLETED);
    }

    @Test
    void cancelExecution_ExistingId_ShouldReturnOk() {
        TestExecutionController controller = createController();
        ResponseEntity<Void> result = controller.cancelExecution(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(executionService).cancelExecution(1L);
    }

    @Test
    void getExecution_ExistingId_ShouldReturnOk() {
        TestScenario scenario = TestScenario.builder().id(1L).name("test").build();
        TestExecution execution = TestExecution.builder()
            .id(1L)
            .scenario(scenario)
            .status(TestStatus.COMPLETED)
            .executedRequests(100L)
            .successfulRequests(95L)
            .failedRequests(5L)
            .build();

        TestExecutionResponse response = TestExecutionResponse.builder()
            .id(1L)
            .scenarioId(1L)
            .scenarioName("test")
            .status(TestStatus.COMPLETED)
            .executedRequests(100L)
            .successfulRequests(95L)
            .failedRequests(5L)
            .build();

        when(executionService.getExecution(1L)).thenReturn(execution);
        when(executionMapper.toResponse(execution)).thenReturn(response);

        TestExecutionController controller = createController();
        ResponseEntity<TestExecutionResponse> result = controller.getExecution(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(1L);
    }

    @Test
    void getRunningExecutions_ShouldReturnRunning() {
        TestScenario scenario = TestScenario.builder().id(1L).name("test").build();
        TestExecution execution = TestExecution.builder()
            .id(1L)
            .scenario(scenario)
            .status(TestStatus.RUNNING)
            .executedRequests(50L)
            .successfulRequests(48L)
            .failedRequests(2L)
            .build();

        TestExecutionResponse response = TestExecutionResponse.builder()
            .id(1L)
            .scenarioId(1L)
            .scenarioName("test")
            .status(TestStatus.RUNNING)
            .executedRequests(50L)
            .successfulRequests(48L)
            .failedRequests(2L)
            .build();

        when(executionService.getRunningExecutions()).thenReturn(List.of(execution));
        when(executionMapper.toResponse(execution)).thenReturn(response);

        TestExecutionController controller = createController();
        ResponseEntity<List<TestExecutionResponse>> result = controller.getRunningExecutions();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody().get(0).getStatus()).isEqualTo(TestStatus.RUNNING);
    }
}