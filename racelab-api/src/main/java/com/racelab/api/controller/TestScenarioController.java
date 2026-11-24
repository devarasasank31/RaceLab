package com.racelab.api.controller;

import com.racelab.api.dto.PageResponse;
import com.racelab.api.dto.TestScenarioRequest;
import com.racelab.api.dto.TestScenarioResponse;
import com.racelab.api.mapper.TestScenarioMapper;
import com.racelab.engine.service.TestScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing test scenarios.
 */
@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
@Tag(name = "Test Scenarios", description = "Manage concurrency test scenarios")
public class TestScenarioController {

    private final TestScenarioService scenarioService;
    private final TestScenarioMapper scenarioMapper;

    @PostMapping
    @Operation(summary = "Create a new test scenario")
    public ResponseEntity<TestScenarioResponse> createScenario(@Valid @RequestBody TestScenarioRequest request) {
        com.racelab.core.domain.TestScenario scenario = scenarioMapper.toEntity(request);
        com.racelab.core.domain.TestScenario saved = scenarioService.createScenario(scenario);
        return ResponseEntity.status(HttpStatus.CREATED).body(scenarioMapper.toResponse(saved));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a test scenario by ID")
    public ResponseEntity<TestScenarioResponse> getScenario(
            @Parameter(description = "Scenario ID") @PathVariable Long id) {
        com.racelab.core.domain.TestScenario scenario = scenarioService.getScenario(id);
        return ResponseEntity.ok(scenarioMapper.toResponse(scenario));
    }

    @GetMapping
    @Operation(summary = "Get all test scenarios with pagination")
    public ResponseEntity<PageResponse<TestScenarioResponse>> getAllScenarios(Pageable pageable) {
        Page<com.racelab.core.domain.TestScenario> page = scenarioService.getAllScenarios(pageable);
        List<TestScenarioResponse> content = page.getContent().stream()
            .map(scenarioMapper::toResponse)
            .toList();

        PageResponse<TestScenarioResponse> response = PageResponse.<TestScenarioResponse>builder()
            .content(content)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .empty(page.isEmpty())
            .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search scenarios by name")
    public ResponseEntity<List<TestScenarioResponse>> searchScenarios(
            @Parameter(description = "Search query") @RequestParam String q) {
        List<com.racelab.core.domain.TestScenario> scenarios = scenarioService.searchScenarios(q);
        List<TestScenarioResponse> response = scenarios.stream()
            .map(scenarioMapper::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a test scenario")
    public ResponseEntity<TestScenarioResponse> updateScenario(
            @Parameter(description = "Scenario ID") @PathVariable Long id,
            @Valid @RequestBody TestScenarioRequest request) {
        com.racelab.core.domain.TestScenario updated = scenarioMapper.toEntity(request);
        com.racelab.core.domain.TestScenario saved = scenarioService.updateScenario(id, updated);
        return ResponseEntity.ok(scenarioMapper.toResponse(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a test scenario")
    public ResponseEntity<Void> deleteScenario(
            @Parameter(description = "Scenario ID") @PathVariable Long id) {
        scenarioService.deleteScenario(id);
        return ResponseEntity.noContent().build();
    }
}