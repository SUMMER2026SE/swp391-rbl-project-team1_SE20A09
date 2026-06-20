package com.sportvenue.repository;

import com.sportvenue.dto.response.AdminOwnerResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.ApprovedStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** Tìm owner theo đường dẫn ảnh đăng ký kinh doanh hoặc ảnh CCCD chứa chuỗi tìm kiếm (tên file). */
    Optional<Owner> findByBusinessLicenseUrlContainingOrIdentityCardUrlContaining(String licenseUrl, String identityUrl);

    /** Kiểm tra xem tên file tài liệu có tồn tại trong bất kỳ hồ sơ đối tác nào không. */
    boolean existsByBusinessLicenseUrlContainingOrIdentityCardUrlContaining(String licenseUrl, String identityUrl);

    @Query("""
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
    Page<AdminOwnerResponse> findOwnersForAdmin(
            @Param("search") String search,
            @Param("accountStatus") AccountStatus accountStatus,
            @Param("approvedStatus") ApprovedStatus approvedStatus,
            Pageable pageable
    );
}
