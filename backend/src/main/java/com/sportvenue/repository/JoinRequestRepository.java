package com.sportvenue.repository;

import com.sportvenue.entity.JoinRequest;
import com.sportvenue.entity.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JoinRequestRepository extends JpaRepository<JoinRequest, Integer> {

    @EntityGraph(attributePaths = {"user", "matchRequest", "matchRequest.stadium", "matchRequest.user", "matchRequest.sportType"})
    List<JoinRequest> findAllByMatchRequestMatchId(Integer matchId);

    @EntityGraph(attributePaths = {"user", "matchRequest", "matchRequest.stadium", "matchRequest.user", "matchRequest.sportType"})
    List<JoinRequest> findAllByUserEmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"user", "matchRequest", "matchRequest.stadium", "matchRequest.sportType"})
    List<JoinRequest> findAllByUserUserIdOrderByCreatedAtDesc(Integer userId);

    @EntityGraph(attributePaths = {"user", "matchRequest"})
    Optional<JoinRequest> findByJoinId(Integer joinId);

    boolean existsByMatchRequestMatchIdAndUserUserIdAndRequestStatusIn(
            Integer matchId, Integer userId, List<JoinRequestStatus> statuses);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE JoinRequest j SET j.requestStatus = :status " +
            "WHERE j.matchRequest.matchId = :matchId " +
            "AND j.requestStatus IN :currentStatuses")
    int bulkUpdateStatus(
            @Param("matchId") Integer matchId,
            @Param("status") JoinRequestStatus status,
            @Param("currentStatuses") List<JoinRequestStatus> currentStatuses);
}
