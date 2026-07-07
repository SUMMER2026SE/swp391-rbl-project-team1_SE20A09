package com.sportvenue.repository;

import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.enums.ComplexStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

@Repository
public interface StadiumComplexRepository extends JpaRepository<StadiumComplex, Integer>, JpaSpecificationExecutor<StadiumComplex> {

    @EntityGraph(attributePaths = {"owner", "owner.user", "sportTypes", "amenities", "images"})
    @Override
    List<StadiumComplex> findAll(Specification<StadiumComplex> spec);

    @EntityGraph(attributePaths = {"owner", "owner.user", "sportTypes", "amenities", "images"})
    @Override
    Page<StadiumComplex> findAll(Specification<StadiumComplex> spec, Pageable pageable);

    List<StadiumComplex> findByOwnerOwnerIdAndComplexStatusNot(Integer ownerId, ComplexStatus status);

    List<StadiumComplex> findByOwnerOwnerId(Integer ownerId);

    /** Dùng bởi LocationBackfillRunner để tìm complex chưa được chuẩn hoá province/district. */
    List<StadiumComplex> findAllByProvinceIsNull();

    @EntityGraph(attributePaths = {"owner", "owner.user", "sportTypes", "amenities", "images"})
    @Query("SELECT sc FROM StadiumComplex sc WHERE sc.complexId = :complexId")
    Optional<StadiumComplex> findWithDetailsByComplexId(@Param("complexId") Integer complexId);

    @Query("""
            SELECT COUNT(sc) FROM StadiumComplex sc
            JOIN sc.owner o
            JOIN o.user u
            WHERE u.email = :ownerEmail
            """)
    long countComplexesByOwnerEmail(@Param("ownerEmail") String ownerEmail);
}
