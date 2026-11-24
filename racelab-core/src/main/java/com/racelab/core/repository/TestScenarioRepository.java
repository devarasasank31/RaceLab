package com.racelab.core.repository;

import com.racelab.core.domain.TestScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TestScenario entities.
 */
@Repository
public interface TestScenarioRepository extends JpaRepository<TestScenario, Long> {

    Optional<TestScenario> findByName(String name);

    @Query("SELECT s FROM TestScenario s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<TestScenario> searchByName(@Param("search") String search);

    @Query("SELECT s FROM TestScenario s ORDER BY s.createdAt DESC")
    List<TestScenario> findAllOrderByCreatedAtDesc();
}