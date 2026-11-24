package com.racelab.api.mapper;

import com.racelab.api.dto.TestScenarioRequest;
import com.racelab.api.dto.TestScenarioResponse;
import com.racelab.core.domain.TestScenario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for TestScenario entity and DTOs.
 */
@Mapper(componentModel = "spring")
public interface TestScenarioMapper {

    TestScenarioMapper INSTANCE = Mappers.getMapper(TestScenarioMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    TestScenario toEntity(TestScenarioRequest request);

    TestScenarioResponse toResponse(TestScenario entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(TestScenarioRequest request, TestScenario entity);
}