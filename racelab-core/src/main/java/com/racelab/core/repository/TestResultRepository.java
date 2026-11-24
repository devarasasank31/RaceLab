package com.racelab.core.repository;

import com.racelab.core.domain.TestExecution;
import com.racelab.core.domain.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for TestResult entities.
 */
@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    Optional<TestResult> findByExecution(TestExecution execution);

    void deleteByExecution(TestExecution execution);
}