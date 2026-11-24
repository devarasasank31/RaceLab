package com.racelab.engine.service;

import com.racelab.core.domain.TestScenario;
import com.racelab.core.domain.HttpMethod;
import com.racelab.core.exception.ScenarioNotFoundException;
import com.racelab.core.repository.TestScenarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestScenarioService.
 */
@ExtendWith(MockitoExtension.class)
class TestScenarioServiceTest {

    @Mock
    private TestScenarioRepository scenarioRepository;

    private TestScenarioService createService() {
        return new TestScenarioService(scenarioRepository);
    }

    @Test
    void createScenario_ValidScenario_ShouldSaveAndReturn() {
        TestScenario scenario = TestScenario.builder()
            .name("new-scenario")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        when(scenarioRepository.existsByName("new-scenario")).thenReturn(false);
        when(scenarioRepository.save(any(TestScenario.class))).thenAnswer(inv -> inv.getArgument(0));

        TestScenarioService service = createService();
        TestScenario result = service.createScenario(scenario);

        assertThat(result.getName()).isEqualTo("new-scenario");
        verify(scenarioRepository).save(any(TestScenario.class));
    }

    @Test
    void createScenario_DuplicateName_ShouldThrow() {
        TestScenario scenario = TestScenario.builder()
            .name("existing-scenario")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        when(scenarioRepository.existsByName("existing-scenario")).thenReturn(true);

        TestScenarioService service = createService();

        assertThatThrownBy(() -> service.createScenario(scenario))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void getScenario_ExistingId_ShouldReturnScenario() {
        TestScenario scenario = TestScenario.builder()
            .name("test")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));

        TestScenarioService service = createService();
        TestScenario result = service.getScenario(1L);

        assertThat(result).isEqualTo(scenario);
    }

    @Test
    void getScenario_NonExistingId_ShouldThrow() {
        when(scenarioRepository.findById(999L)).thenReturn(Optional.empty());

        TestScenarioService service = createService();

        assertThatThrownBy(() -> service.getScenario(999L))
            .isInstanceOf(ScenarioNotFoundException.class);
    }

    @Test
    void updateScenario_ValidUpdate_ShouldSaveAndReturn() {
        TestScenario existing = TestScenario.builder()
            .id(1L)
            .name("old-name")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        TestScenario updated = TestScenario.builder()
            .name("new-name")
            .concurrentUsers(20)
            .totalRequests(200)
            .targetEndpoint("http://localhost:8080/api/updated")
            .httpMethod(HttpMethod.PUT)
            .build();

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(scenarioRepository.save(any(TestScenario.class))).thenAnswer(inv -> inv.getArgument(0));

        TestScenarioService service = createService();
        TestScenario result = service.updateScenario(1L, updated);

        assertThat(result.getName()).isEqualTo("new-name");
        assertThat(result.getConcurrentUsers()).isEqualTo(20);
        assertThat(result.getTotalRequests()).isEqualTo(200);
        assertThat(result.getTargetEndpoint()).isEqualTo("http://localhost:8080/api/updated");
        assertThat(result.getHttpMethod()).isEqualTo(HttpMethod.PUT);
    }

    @Test
    void deleteScenario_ExistingId_ShouldDelete() {
        TestScenario scenario = TestScenario.builder()
            .id(1L)
            .name("to-delete")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));

        TestScenarioService service = createService();
        service.deleteScenario(1L);

        verify(scenarioRepository).delete(scenario);
    }

    @Test
    void getAllScenarios_ShouldReturnAll() {
        TestScenario s1 = TestScenario.builder().id(1L).name("s1").concurrentUsers(10).totalRequests(100).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build();
        TestScenario s2 = TestScenario.builder().id(2L).name("s2").concurrentUsers(20).totalRequests(200).targetEndpoint("http://localhost:8080/api/test").httpMethod(HttpMethod.POST).build();

        when(scenarioRepository.findAllOrderByCreatedAtDesc()).thenReturn(List.of(s1, s2));

        TestScenarioService service = createService();
        List<TestScenario> result = service.getAllScenarios();

        assertThat(result).hasSize(2);
    }

    @Test
    void exists_ExistingId_ShouldReturnTrue() {
        when(scenarioRepository.existsById(1L)).thenReturn(true);

        TestScenarioService service = createService();
        assertThat(service.exists(1L)).isTrue();
    }

    @Test
    void existsByName_ExistingName_ShouldReturnTrue() {
        when(scenarioRepository.existsByName("test")).thenReturn(true);

        TestScenarioService service = createService();
        assertThat(service.existsByName("test")).isTrue();
    }
}