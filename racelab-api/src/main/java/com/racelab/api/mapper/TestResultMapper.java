package com.racelab.api.mapper;

import com.racelab.api.dto.TestResultResponse;
import com.racelab.core.domain.TestResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for TestResult entity and DTO.
 */
@Mapper(componentModel = "spring")
public interface TestResultMapper {

    TestResultMapper INSTANCE = Mappers.getMapper(TestResultMapper.class);

    @Mapping(target = "executionId", source = "execution.id")
    TestResultResponse toResponse(TestResult entity);
}