package com.sportvenue.repository;

import com.sportvenue.entity.Report;
import com.sportvenue.entity.enums.ReportCategory;
import com.sportvenue.entity.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {

    @EntityGraph(attributePaths = {
            "reporter", "reporter.role", "reportee", "reportee.role",
            "booking", "matchRequest", "joinRequest", "stadium", "resolvedBy", "resolvedBy.role"
    })
    Optional<Report> findWithDetailsByReportId(Integer reportId);

    @EntityGraph(attributePaths = {
            "reporter", "reporter.role", "reportee", "reportee.role",
            "booking", "matchRequest", "joinRequest", "stadium", "resolvedBy", "resolvedBy.role"
    })
    Page<Report> findByReporterUserIdOrderByCreatedAtDesc(Integer reporterId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "reporter", "reporter.role", "reportee", "reportee.role",
            "booking", "matchRequest", "joinRequest", "stadium", "resolvedBy", "resolvedBy.role"
    })
    @Query("""
            SELECT r FROM Report r
            WHERE (:status IS NULL OR r.status = :status)
            AND (:category IS NULL OR r.category = :category)
            """)
    Page<Report> findForAdmin(
            @Param("status") ReportStatus status,
            @Param("category") ReportCategory category,
            Pageable pageable);

    long countByReporterUserIdAndCreatedAtBetween(
            Integer reporterId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay);
}
