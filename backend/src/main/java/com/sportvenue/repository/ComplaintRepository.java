package com.sportvenue.repository;

import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    /** Lấy toàn bộ khiếu nại của các sân thuộc quản lý của một Owner. */
    @EntityGraph(attributePaths = {"user", "booking", "booking.stadium"})
    List<Complaint> findByBookingStadiumOwnerUserEmailOrderByCreatedAtDesc(String email);

    /** Lấy toàn bộ khiếu nại của một khách hàng (không phân trang). */
    @EntityGraph(attributePaths = {"user", "booking", "booking.stadium"})
    List<Complaint> findByUserUserIdOrderByCreatedAtDesc(Integer userId);

    /** Kiểm tra xem đơn đặt sân đã được khiếu nại chưa. */
    boolean existsByBookingBookingId(Integer bookingId);

    /** Lấy toàn bộ khiếu nại trên hệ thống cho Admin, sử dụng EntityGraph để tránh N+1. */
    @EntityGraph(attributePaths = {"user", "booking", "booking.stadium", "booking.stadium.owner", "booking.stadium.owner.user"})
    List<Complaint> findAllByOrderByCreatedAtDesc();
}
