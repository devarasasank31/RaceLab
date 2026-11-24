package com.racelab.api.controller;

import com.racelab.api.dto.TestScenarioRequest;
import com.racelab.api.mapper.TestScenarioMapper;
import com.racelab.core.domain.HttpMethod;
import com.racelab.core.domain.TestScenario;
import com.racelab.core.exception.ScenarioNotFoundException;
import com.racelab.engine.service.TestScenarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestScenarioController.
 */
@ExtendWith(MockitoExtension.class)
class TestScenarioControllerTest {

    @Mock
    private TestScenarioService scenarioService;

    @Mock
    private TestScenarioMapper scenarioMapper;

    private TestScenarioController createController() {
        return new TestScenarioController(scenarioService, scenarioMapper);
    }

    @Test
    void createScenario_ValidRequest_ShouldReturnCreated() {
        TestScenarioRequest request = new TestScenarioRequest();
        request.setName("new-scenario");
        request.setConcurrentUsers(10);
        request.setTotalRequests(100);
        request.setTargetEndpoint("http://localhost:8080/api/test");
        request.setHttpMethod(HttpMethod.POST);

        TestScenario scenario = TestScenario.builder()
            .name("new-scenario")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        TestScenario saved = TestScenario.builder()
            .id(1L)
            .name("new-scenario")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        com.racelab.api.dto.TestScenarioResponse response = com.racelab.api.dto.TestScenarioResponse.builder()
            .id(1L)
            .name("new-scenario")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        when(scenarioService.createScenario(any(TestScenario.class))).thenReturn(saved);
        when(scenarioMapper.toResponse(saved)).thenReturn(response);

        TestScenarioController controller = createController();
        ResponseEntity<com.racelab.api.dto.TestScenarioResponse> result = controller.createScenario(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(1L);
        assertThat(result.getBody().getName()).isEqualTo("new-scenario");
    }

    @Test
    void getScenario_ExistingId_ShouldReturnOk() {
        TestScenario scenario = TestScenario.builder()
            .id(1L)
            .name("test-scenario")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        com.racelab.api.dto.TestScenarioResponse response = com.racelab.api.dto.TestScenarioResponse.builder()
            .id(1L)
            .name("test-scenario")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        when(scenarioService.getScenario(1L)).thenReturn(scenario);
        when(scenarioMapper.toResponse(scenario)).thenReturn(response);

        TestScenarioController controller = createController();
        ResponseEntity<com.racelab.api.dto.TestScenarioResponse> result = controller.getScenario(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(1L);
    }

    @Test
    void getAllScenarios_ShouldReturnPage() {
        TestScenario s1 = TestScenario.builder().id(1L).name("s1").concurrentUsers(10).totalRequests(100).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build();
        TestScenario s2 = TestScenario.builder().id(2L).name("s2").concurrentUsers(20).totalRequests(200).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build();

        com.racelab.api.dto.TestScenarioResponse r1 = com.racelab.api.dto.TestScenarioResponse.builder().id(1L).name("s1").concurrentUsers(10).totalRequests(100).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build();
        com.racelab.api.dto.TestScenarioResponse r2 = com.racelab.api.dto.TestScenarioResponse.builder().id(2L).name("s2").concurrentUsers(20).totalRequests(200).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build();

        Page<TestScenario> page = new PageImpl<>(List.of(s1, s2));
        when(scenarioService.getAllScenarios(any(Pageable.class))).thenReturn(page);
        when(scenarioMapper.toResponse(s1)).thenReturn(r1);
        when(scenarioMapper.toResponse(s2)).thenReturn(r2);

        TestScenarioController controller = createController();
        ResponseEntity<com.racelab.api.dto.PageResponse<com.racelab.api.dto.TestScenarioResponse>> result = controller.getAllScenarios(Pageable.unpaged());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(2);
    }

    @Test
    void updateScenario_ValidRequest_ShouldReturnUpdated() {
        TestScenarioRequest request = new TestScenarioRequest();
        request.setName("updated");
        request.setConcurrentUsers(20);

        TestScenario updated = TestScenario.builder()
            .id(1L)
            .name("updated")
            .concurrentUsers(20)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        com.racelab.api.dto.TestScenarioResponse response = com.racelab.api.dto.TestScenarioResponse.builder()
            .id(1L)
            .name("updated")
            .concurrentUsers(20)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        when(scenarioService.updateScenario(eq(1L), any(TestScenario.class))).thenReturn(updated);
        when(scenarioMapper.toResponse(updated)).thenReturn(response);

        TestScenarioController controller = createController();
        ResponseEntity<com.racelab.api.dto.TestScenarioResponse> result = controller.updateScenario(1L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getName()).isEqualTo("updated");
        assertThat(result.getBody().getConcurrentUsers()).isEqualTo(20);
    }

    @Test
    void deleteScenario_ExistingId_ShouldReturnNoContent() {
        TestScenarioController controller = createController();
        ResponseEntity<Void> result = controller.deleteScenario(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(scenarioService).deleteScenario(1L);
    }
}