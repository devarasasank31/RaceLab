package com.racelab.engine;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Test configuration for engine module.
 * Imports only necessary beans for testing.
 */
@Configuration
@Import({
    EngineProperties.class,
    ConcurrencyTestRunner.class,
    ResultAggregator.class,
    WebClientHttpClientService.class,
    TestExecutionService.class,
    TestScenarioService.class,
    com.racelab.core.util.ConcurrencyUtils.class
})
public class TestEngineConfiguration {
}