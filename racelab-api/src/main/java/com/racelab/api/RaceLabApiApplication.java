package com.racelab.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * RaceLab API Application entry point.
 */
@SpringBootApplication(scanBasePackages = "com.racelab")
@EntityScan(basePackages = "com.racelab.core.domain")
@EnableJpaRepositories(basePackages = "com.racelab.core.repository")
@EnableAsync
public class RaceLabApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RaceLabApiApplication.class, args);
    }
}