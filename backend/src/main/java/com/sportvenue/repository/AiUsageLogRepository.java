package com.sportvenue.repository;

import com.sportvenue.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    @Query("SELECT COUNT(a) FROM AiUsageLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    long countTotalRequests(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(a.latencyMs) FROM AiUsageLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageLatency(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(a.confidence) FROM AiUsageLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageConfidence(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a.parsedIntent, COUNT(a) FROM AiUsageLog a WHERE a.createdAt BETWEEN :startDate AND :endDate GROUP BY a.parsedIntent")
    List<Object[]> getIntentDistribution(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM AiUsageLog a WHERE a.validationResult != 'PASS' AND a.createdAt BETWEEN :startDate AND :endDate")
    long countValidationErrors(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
