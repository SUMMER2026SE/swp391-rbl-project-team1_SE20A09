package com.sportvenue.repository;

import com.sportvenue.entity.JoinRequest;
import com.sportvenue.entity.enums.JoinRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JoinRequestRepository extends JpaRepository<JoinRequest, Integer> {

    @EntityGraph(attributePaths = {"user", "matchRequest", "matchRequest.stadium", "matchRequest.user", "matchRequest.sportType"})
    List<JoinRequest> findAllByMatchRequestMatchId(Integer matchId);

    @EntityGraph(attributePaths = {"user", "matchRequest", "matchRequest.stadium", "matchRequest.user", "matchRequest.sportType"})
    Page<JoinRequest> findAllByUserEmailOrderByCreatedAtDesc(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "matchRequest", "matchRequest.stadium", "matchRequest.sportType"})
    List<JoinRequest> findAllByUserUserIdOrderByCreatedAtDesc(Integer userId);

    @EntityGraph(attributePaths = {"user", "matchRequest"})
    Optional<JoinRequest> findByJoinId(Integer joinId);

    boolean existsByMatchRequestMatchIdAndUserUserIdAndRequestStatusIn(
            Integer matchId, Integer userId, List<JoinRequestStatus> statuses);

    /** Kiểm tra user đã được APPROVED vào một kèo trùng thời gian chưa. */
    @Query("""
            SELECT COUNT(j) > 0 FROM JoinRequest j
            WHERE j.user.userId = :userId
            AND j.requestStatus = com.sportvenue.entity.enums.JoinRequestStatus.APPROVED
            AND j.matchRequest.playDate = :playDate
            AND j.matchRequest.startTime < :endTime
            AND j.matchRequest.endTime > :startTime
            """)
    boolean existsApprovedOverlappingJoinRequest(
            @Param("userId") Integer userId,
            @Param("playDate") LocalDate playDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE JoinRequest j SET j.requestStatus = :status " +
            "WHERE j.matchRequest.matchId = :matchId " +
            "AND j.requestStatus IN :currentStatuses")
    int bulkUpdateStatus(
            @Param("matchId") Integer matchId,
            @Param("status") JoinRequestStatus status,
            @Param("currentStatuses") List<JoinRequestStatus> currentStatuses);
}
