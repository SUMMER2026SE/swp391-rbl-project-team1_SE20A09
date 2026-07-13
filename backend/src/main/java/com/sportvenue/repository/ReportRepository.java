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
import java.util.List;
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

    long countByReporteeUserIdAndCategoryAndStatus(
            Integer reporteeId,
            ReportCategory category,
            ReportStatus status);

    @Query("""
            SELECT COUNT(r) FROM Report r
            WHERE r.createdAt BETWEEN :start AND :end
            AND (:role IS NULL OR r.reportee.role.roleName = :role)
            AND (:status IS NULL OR r.status = :status)
            AND (:category IS NULL OR r.category = :category)
            """)
    long countModerationReports(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("role") String role,
            @Param("status") ReportStatus status,
            @Param("category") ReportCategory category);

    @Query("""
            SELECT r.status, COUNT(r) FROM Report r
            WHERE r.createdAt BETWEEN :start AND :end
            AND (:role IS NULL OR r.reportee.role.roleName = :role)
            AND (:status IS NULL OR r.status = :status)
            AND (:category IS NULL OR r.category = :category)
            GROUP BY r.status
            """)
    List<Object[]> countModerationReportsByStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("role") String role,
            @Param("status") ReportStatus status,
            @Param("category") ReportCategory category);

    @Query("""
            SELECT r.category, COUNT(r) FROM Report r
            WHERE r.createdAt BETWEEN :start AND :end
            AND (:role IS NULL OR r.reportee.role.roleName = :role)
            AND (:status IS NULL OR r.status = :status)
            AND (:category IS NULL OR r.category = :category)
            GROUP BY r.category
            """)
    List<Object[]> countModerationReportsByCategory(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("role") String role,
            @Param("status") ReportStatus status,
            @Param("category") ReportCategory category);

    @Query("""
            SELECT FUNCTION('DATE', r.createdAt), COUNT(r) FROM Report r
            WHERE r.createdAt BETWEEN :start AND :end
            AND (:role IS NULL OR r.reportee.role.roleName = :role)
            AND (:status IS NULL OR r.status = :status)
            AND (:category IS NULL OR r.category = :category)
            GROUP BY FUNCTION('DATE', r.createdAt)
            ORDER BY FUNCTION('DATE', r.createdAt) ASC
            """)
    List<Object[]> countModerationReportsByDate(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("role") String role,
            @Param("status") ReportStatus status,
            @Param("category") ReportCategory category);

    @Query("""
            SELECT r.reportee.userId, r.reportee.firstName, r.reportee.lastName,
                   r.reportee.email, r.reportee.role.roleName, COUNT(r)
            FROM Report r
            WHERE r.createdAt BETWEEN :start AND :end
            AND (:role IS NULL OR r.reportee.role.roleName = :role)
            AND (:status IS NULL OR r.status = :status)
            AND (:category IS NULL OR r.category = :category)
            GROUP BY r.reportee.userId, r.reportee.firstName, r.reportee.lastName,
                     r.reportee.email, r.reportee.role.roleName
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> findTopReportedUsersForModeration(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("role") String role,
            @Param("status") ReportStatus status,
            @Param("category") ReportCategory category);

    @Query("""
            SELECT r.createdAt, r.resolvedAt FROM Report r
            WHERE r.createdAt BETWEEN :start AND :end
            AND r.resolvedAt IS NOT NULL
            AND (:role IS NULL OR r.reportee.role.roleName = :role)
            AND (:status IS NULL OR r.status = :status)
            AND (:category IS NULL OR r.category = :category)
            """)
    List<Object[]> findReportResolutionDurationsForModeration(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("role") String role,
            @Param("status") ReportStatus status,
            @Param("category") ReportCategory category);
}
