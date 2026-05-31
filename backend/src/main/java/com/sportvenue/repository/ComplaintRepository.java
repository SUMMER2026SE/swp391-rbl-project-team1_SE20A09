package com.sportvenue.repository;

import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** Lấy tất cả khiếu nại của một sân — dùng cho Owner quản lý. */
    @EntityGraph(attributePaths = {"user", "booking", "booking.stadium"})
    Page<Complaint> findByBookingStadiumStadiumIdOrderByCreatedAtDesc(
            Integer stadiumId, Pageable pageable);

    /** Lấy khiếu nại của nhiều sân — dùng cho Owner xem tất cả. */
    @EntityGraph(attributePaths = {"user", "booking", "booking.stadium"})
    @Query("""
            SELECT c FROM Complaint c
            WHERE c.booking.stadium.stadiumId IN :stadiumIds
            ORDER BY c.createdAt DESC
            """)
    Page<Complaint> findByBookingStadiumStadiumIdInOrderByCreatedAtDesc(
            @Param("stadiumIds") java.util.List<Integer> stadiumIds,
            Pageable pageable);
}
