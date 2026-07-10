package com.sportvenue.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.enums.ComplaintStatus;

/**
 * Repository cho Complaint entity.
 * Enhanced với escalation workflow support.
 */
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Integer> {

    /** Lấy danh sách khiếu nại của một sân theo trạng thái — dùng cho Owner xử lý. */
    @EntityGraph(attributePaths = {"user", "booking"})
    Page<Complaint> findByBookingStadiumStadiumIdAndStatus(
            Integer stadiumId, ComplaintStatus status, Pageable pageable);

    /** Lấy toàn bộ khiếu nại của các sân thuộc quản lý của một Owner (phân trang). */
    @EntityGraph(attributePaths = {"user", "booking", "booking.stadium"})
    Page<Complaint> findByBookingStadiumOwnerUserEmailOrderByCreatedAtDesc(String email, Pageable pageable);

    /** Lấy toàn bộ khiếu nại của một khách hàng (phân trang). */
    @EntityGraph(attributePaths = {"user", "booking", "booking.stadium"})
    Page<Complaint> findByUserUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    /** Lấy toàn bộ khiếu nại trên hệ thống cho Admin (phân trang). */
    @EntityGraph(attributePaths = {"user", "booking", "booking.stadium", "booking.stadium.owner", "booking.stadium.owner.user"})
    Page<Complaint> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ComplaintStatus status);

    /** Kiểm tra xem đơn đặt sân có khiếu nại chưa được giải quyết không. */
    boolean existsByBookingBookingIdAndStatusNot(Integer bookingId, ComplaintStatus status);

    /** Lấy khiếu nại đã quá hạn customer response (cho scheduled task). */
    List<Complaint> findByStatusAndCustomerResponseDeadlineBefore(
            ComplaintStatus status, LocalDateTime deadline);

    /** Lấy khiếu nại vi phạm SLA (cho scheduled task). */
    List<Complaint> findByStatusInAndCreatedAtBeforeAndSlaViolatedFalse(
            List<ComplaintStatus> statuses, LocalDateTime threshold);

    /** Lấy khiếu nại đang chờ Admin xử lý. */
    @EntityGraph(attributePaths = {"user", "booking", "booking.stadium", "booking.stadium.owner"})
    Page<Complaint> findByStatusInOrderByEscalatedAtDesc(
            List<ComplaintStatus> statuses, Pageable pageable);
}

