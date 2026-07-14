package com.sportvenue.repository;

import com.sportvenue.entity.Appeal;
import com.sportvenue.entity.enums.AppealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppealRepository extends JpaRepository<Appeal, Integer> {

    boolean existsByUserUserIdAndStatus(Integer userId, AppealStatus status);

    @EntityGraph(attributePaths = {"user", "reviewedBy"})
    Optional<Appeal> findTopByUserUserIdOrderByCreatedAtDesc(Integer userId);

    @Override
    @EntityGraph(attributePaths = {"user", "reviewedBy"})
    Optional<Appeal> findById(Integer appealId);

    @EntityGraph(attributePaths = {"user", "reviewedBy"})
    Page<Appeal> findByStatus(AppealStatus status, Pageable pageable);
}
