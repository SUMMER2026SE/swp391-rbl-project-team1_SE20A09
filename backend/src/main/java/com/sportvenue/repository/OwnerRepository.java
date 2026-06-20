package com.sportvenue.repository;

import com.sportvenue.entity.Owner;
import com.sportvenue.entity.enums.ApprovedStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho Owner entity.
 * Stub — các thành viên mở rộng thêm query khi cần.
 */
@Repository
public interface OwnerRepository extends JpaRepository<Owner, Integer> {

    /** Tìm owner profile theo userId — dùng khi đăng nhập cần kiểm tra owner profile. */
    @EntityGraph(attributePaths = {"user"})
    Optional<Owner> findByUserUserId(Integer userId);

    /** Tìm owner profile theo email của User. */
    @EntityGraph(attributePaths = {"user"})
    Optional<Owner> findByUserEmail(String email);

    /** Kiểm tra user đã có owner profile chưa. */
    boolean existsByUserUserId(Integer userId);

    /** Đếm số owner theo trạng thái phê duyệt — dùng cho Admin dashboard. */
    long countByApprovedStatus(ApprovedStatus approvedStatus);

    @org.springframework.data.jpa.repository.Query("""
        SELECT new com.sportvenue.dto.response.AdminOwnerResponse(
            o.ownerId, u.userId, CONCAT(u.firstName, ' ', u.lastName),
            u.email, u.phoneNumber, o.businessName, o.taxCode,
            o.businessAddress, CAST(o.approvedStatus AS string), CAST(u.accountStatus AS string), o.createdAt
        )
        FROM Owner o
        JOIN o.user u
        WHERE (:search IS NULL OR 
               LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR 
               LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
               LOWER(o.businessName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
               LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (:accountStatus IS NULL OR u.accountStatus = :accountStatus)
          AND (:approvedStatus IS NULL OR o.approvedStatus = :approvedStatus)
    """)
    org.springframework.data.domain.Page<com.sportvenue.dto.response.AdminOwnerResponse> findOwnersForAdmin(
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("accountStatus") com.sportvenue.entity.enums.AccountStatus accountStatus,
            @org.springframework.data.repository.query.Param("approvedStatus") ApprovedStatus approvedStatus,
            org.springframework.data.domain.Pageable pageable
    );
}
