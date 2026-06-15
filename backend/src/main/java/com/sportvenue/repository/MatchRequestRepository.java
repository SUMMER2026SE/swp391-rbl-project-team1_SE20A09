package com.sportvenue.repository;

import com.sportvenue.entity.MatchRequest;
import com.sportvenue.entity.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRequestRepository extends JpaRepository<MatchRequest, Integer> {

    /** Tìm các kèo ghép có kèm thông tin sân, môn thể thao và người tạo kèm phân trang */
    @EntityGraph(attributePaths = {"user", "stadium", "sportType"})
    Page<MatchRequest> findAllByMatchStatus(MatchStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "stadium", "sportType"})
    List<MatchRequest> findAllByPlayDateGreaterThanEqualAndMatchStatus(LocalDate date, MatchStatus status);

    @EntityGraph(attributePaths = {"user", "stadium", "sportType"})
    List<MatchRequest> findAllByUserEmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"user", "stadium", "sportType"})
    Optional<MatchRequest> findByMatchId(Integer matchId);
}
