package com.sportvenue.repository;

import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cho Complaint entity.
 * Stub — Hoàng mở rộng thêm khi implement UC-OWN-09.
 */
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Integer> {

    /** Lấy danh sách khiếu nại của một sân theo trạng thái — dùng cho Owner xử lý. */
    @EntityGraph(attributePaths = {"user", "booking"})
    Page<Complaint> findByBookingStadiumStadiumIdAndStatus(
            Integer stadiumId, ComplaintStatus status, Pageable pageable);

    /** Lấy khiếu nại của một user. */
    Page<Complaint> findByUserUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
