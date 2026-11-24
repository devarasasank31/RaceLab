package com.racelab.engine.service;

import com.racelab.core.domain.TestScenario;
import com.racelab.core.exception.ScenarioNotFoundException;
import com.racelab.core.repository.TestScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing test scenarios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestScenarioService {

    private final TestScenarioRepository scenarioRepository;

    /**
     * Create a new test scenario.
     */
    @Transactional
    public TestScenario createScenario(TestScenario scenario) {
        if (scenarioRepository.existsByName(scenario.getName())) {
            throw new IllegalArgumentException("Scenario with name '" + scenario.getName() + "' already exists");
        }
        scenario.validateConfiguration();
        return scenarioRepository.save(scenario);
    }

    /**
     * Get a scenario by ID.
     */
    @Transactional(readOnly = true)
    public TestScenario getScenario(Long id) {
        return scenarioRepository.findById(id)
            .orElseThrow(() -> new ScenarioNotFoundException(id));
    }

    /**
     * Get a scenario by name.
     */
    @Transactional(readOnly = true)
    public TestScenario getScenarioByName(String name) {
        return scenarioRepository.findByName(name)
            .orElseThrow(() -> new ScenarioNotFoundException(name));
    }

    /**
     * Get all scenarios with pagination.
     */
    @Transactional(readOnly = true)
    public Page<TestScenario> getAllScenarios(Pageable pageable) {
        return scenarioRepository.findAll(pageable);
    }

    /**
     * Get all scenarios ordered by creation date (latest first).
     */
    @Transactional(readOnly = true)
    public List<TestScenario> getAllScenarios() {
        return scenarioRepository.findAllOrderByCreatedAtDesc();
    }

    /**
     * Search scenarios by name.
     */
    @Transactional(readOnly = true)
    public List<TestScenario> searchScenarios(String query) {
        return scenarioRepository.searchByName(query);
    }

    /**
     * Update a scenario.
     */
    @Transactional
    public TestScenario updateScenario(Long id, TestScenario updated) {
        TestScenario existing = getScenario(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setConcurrentUsers(updated.getConcurrentUsers());
        existing.setTotalRequests(updated.getTotalRequests());
        existing.setDurationSeconds(updated.getDurationSeconds());
        existing.setRampUpSeconds(updated.getRampUpSeconds());
        existing.setTargetEndpoint(updated.getTargetEndpoint());
        existing.setHttpMethod(updated.getHttpMethod());
        existing.setRequestHeaders(updated.getRequestHeaders());
        existing.setRequestPayload(updated.getRequestPayload());
        existing.setConcurrencyStrategy(updated.getConcurrencyStrategy());
        existing.setStrategyConfig(updated.getStrategyConfig());
        existing.setRequestTimeoutSeconds(updated.getRequestTimeoutSeconds());
        existing.setMaxRetries(updated.getMaxRetries());
        existing.validateConfiguration();
        return scenarioRepository.save(existing);
    }

    /**
     * Delete a scenario.
     */
    @Transactional
    public void deleteScenario(Long id) {
        TestScenario scenario = getScenario(id);
        scenarioRepository.delete(scenario);
    }

    /**
     * Check if a scenario exists.
     */
    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return scenarioRepository.existsById(id);
    }

    /**
     * Check if a scenario name exists.
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return scenarioRepository.existsByName(name);
    }
}