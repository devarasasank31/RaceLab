package com.racelab.core.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for TestScenario domain entity validation.
 */
class TestScenarioTest {

    @Test
    void builder_WithValidConfig_ShouldCreateScenario() {
        TestScenario scenario = TestScenario.builder()
            .name("valid-test")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        assertThat(scenario.getName()).isEqualTo("valid-test");
        assertThat(scenario.getConcurrentUsers()).isEqualTo(10);
        assertThat(scenario.getTotalRequests()).isEqualTo(100);
        assertThat(scenario.isRequestCountBased()).isTrue();
        assertThat(scenario.isDurationBased()).isFalse();
    }

    @Test
    void builder_WithDuration_ShouldCreateDurationBasedScenario() {
        TestScenario scenario = TestScenario.builder()
            .name("duration-test")
            .concurrentUsers(10)
            .durationSeconds(60)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.GET)
            .build();

        assertThat(scenario.isDurationBased()).isTrue();
        assertThat(scenario.isRequestCountBased()).isFalse();
    }

    @Test
    void builder_WithBothDurationAndRequests_ShouldBeBoth() {
        TestScenario scenario = TestScenario.builder()
            .name("both-test")
            .concurrentUsers(10)
            .totalRequests(100)
            .durationSeconds(60)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod.HttpMethod.POST)
            .build();

        assertThat(scenario.isDurationBased()).isTrue();
        assertThat(scenario.isRequestCountBased()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(HttpMethod.class)
    void builder_WithAllHttpMethods_ShouldAccept(HttpMethod method) {
        TestScenario scenario = TestScenario.builder()
            .name("method-test")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(method)
            .build();

        assertThat(scenario.getHttpMethod()).isEqualTo(method);
    }

    @Test
    void validateConfiguration_NoDurationNoRequests_ShouldThrow() {
        TestScenario scenario = TestScenario.builder()
            .name("invalid-test")
            .concurrentUsers(10)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        assertThatThrownBy(scenario::validateConfiguration)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Either durationSeconds or totalRequests must be specified");
    }

    @Test
    void validateConfiguration_RampUpEqualsDuration_ShouldThrow() {
        TestScenario scenario = TestScenario.builder()
            .name("invalid-rampup")
            .concurrentUsers(10)
            .durationSeconds(60)
            .rampUpSeconds(60)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        assertThatThrownBy(scenario::validateConfiguration)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ramp-up period must be less than test duration");
    }

    @Test
    void validateConfiguration_RampUpGreaterThanDuration_ShouldThrow() {
        TestScenario scenario = TestScenario.builder()
            .name("invalid-rampup")
            .concurrentUsers(10)
            .durationSeconds(30)
            .rampUpSeconds(60)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        assertThatThrownBy(scenario::validateConfiguration)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ramp-up period must be less than test duration");
    }

    @Test
    void validateConfiguration_ValidRampUp_ShouldNotThrow() {
        TestScenario scenario = TestScenario.builder()
            .name("valid-rampup")
            .concurrentUsers(10)
            .durationSeconds(60)
            .rampUpSeconds(10)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        scenario.validateConfiguration(); // Should not throw
    }

    @Test
    void defaults_ShouldHaveReasonableValues() {
        TestScenario scenario = TestScenario.builder()
            .name("defaults-test")
            .concurrentUsers(10)
            .totalRequests(100)
            .targetEndpoint("http://localhost:8080/api/test")
            .httpMethod(HttpMethod.POST)
            .build();

        assertThat(scenario.getRequestTimeoutSeconds()).isEqualTo(30);
        assertThat(scenario.getMaxRetries()).isEqualTo(0);
        assertThat(scenario.getConcurrencyStrategy()).isEqualTo(ConcurrencyStrategyType.NONE);
        assertThat(scenario.getRampUpSeconds()).isZero();
    }
}