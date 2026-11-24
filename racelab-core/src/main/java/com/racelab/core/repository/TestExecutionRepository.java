package com.racelab.core.repository;

import com.racelab.core.domain.TestExecution;
import com.racelab.core.domain.TestScenario;
import com.racelab.core.domain.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TestExecution entities.
 */
@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {

    List<TestExecution> findByScenarioOrderByCreatedAtDesc(TestScenario scenario);

    List<TestExecution> findByStatus(TestStatus status);

    List<TestExecution> findByScenarioAndStatus(TestScenario scenario, TestStatus status);

    @Query("SELECT e FROM TestExecution e WHERE e.startedAt >= :since ORDER BY e.startedAt DESC")
    List<TestExecution> findRecentExecutions(@Param("since") Instant since);

    @Query("SELECT e FROM TestExecution e WHERE e.status IN (:statuses)")
    List<TestExecution> findByStatusIn(@Param("statuses") List<TestStatus> statuses);

    Optional<TestExecution> findTopByScenarioOrderByCreatedAtDesc(TestScenario scenario);
}