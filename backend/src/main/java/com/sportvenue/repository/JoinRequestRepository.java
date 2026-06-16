package com.sportvenue.repository;

import com.sportvenue.entity.JoinRequest;
import com.sportvenue.entity.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JoinRequestRepository extends JpaRepository<JoinRequest, Integer> {

    @EntityGraph(attributePaths = {"user", "matchRequest", "matchRequest.stadium"})
    List<JoinRequest> findAllByMatchRequestMatchId(Integer matchId);

    @EntityGraph(attributePaths = {"user", "matchRequest", "matchRequest.stadium"})
    List<JoinRequest> findAllByUserEmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"user", "matchRequest", "matchRequest.stadium", "matchRequest.sportType"})
    List<JoinRequest> findAllByUserUserIdOrderByCreatedAtDesc(Integer userId);

    @EntityGraph(attributePaths = {"user", "matchRequest"})
    Optional<JoinRequest> findByJoinId(Integer joinId);

    boolean existsByMatchRequestMatchIdAndUserUserIdAndRequestStatusIn(
            Integer matchId, Integer userId, List<JoinRequestStatus> statuses);
}
