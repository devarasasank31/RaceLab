package com.racelab.api.mapper;

import com.racelab.api.dto.TestExecutionResponse;
import com.racelab.core.domain.TestExecution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for TestExecution entity and DTO.
 */
@Mapper(componentModel = "spring")
public interface TestExecutionMapper {

    TestExecutionMapper INSTANCE = Mappers.getMapper(TestExecutionMapper.class);

    @Mapping(target = "scenarioId", source = "scenario.id")
    @Mapping(target = "scenarioName", source = "scenario.name")
    TestExecutionResponse toResponse(TestExecution entity);
}