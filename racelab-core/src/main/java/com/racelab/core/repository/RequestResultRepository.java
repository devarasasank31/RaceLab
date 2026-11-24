package com.racelab.core.repository;

import com.racelab.core.domain.RequestResult;
import com.racelab.core.domain.TestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for RequestResult entities.
 */
@Repository
public interface RequestResultRepository extends JpaRepository<RequestResult, Long> {

    List<RequestResult> findByExecutionOrderByTimestampAsc(TestExecution execution);

    List<RequestResult> findByExecutionAndSuccess(TestExecution execution, boolean success);

    @Query("SELECT r FROM RequestResult r WHERE r.execution = :execution AND r.latencyMs IS NOT NULL ORDER BY r.latencyMs ASC")
    List<RequestResult> findByExecutionOrderByLatencyAsc(@Param("execution") TestExecution execution);

    long countByExecutionAndSuccess(TestExecution execution, boolean success);

    long countByExecution(TestExecution execution);

    @Query("SELECT AVG(r.latencyMs) FROM RequestResult r WHERE r.execution = :execution AND r.latencyMs IS NOT NULL")
    Double findAverageLatencyByExecution(@Param("execution") TestExecution execution);

    @Query("SELECT MIN(r.latencyMs) FROM RequestResult r WHERE r.execution = :execution AND r.latencyMs IS NOT NULL")
    Long findMinLatencyByExecution(@Param("execution") TestExecution execution);

    @Query("SELECT MAX(r.latencyMs) FROM RequestResult r WHERE r.execution = :execution AND r.latencyMs IS NOT NULL")
    Long findMaxLatencyByExecution(@Param("execution") TestExecution execution);
}