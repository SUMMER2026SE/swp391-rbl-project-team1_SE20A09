package com.sportvenue.repository;

import com.sportvenue.entity.Owner;
import com.sportvenue.entity.enums.ApprovedStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    /** Lấy danh sách owner profile theo trạng thái phê duyệt — dùng cho Admin duyệt hồ sơ. */
    @EntityGraph(attributePaths = {"user"})
    Page<Owner> findByApprovedStatus(ApprovedStatus approvedStatus, Pageable pageable);
}
