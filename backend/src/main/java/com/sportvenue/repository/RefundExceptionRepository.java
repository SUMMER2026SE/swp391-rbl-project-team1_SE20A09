package com.sportvenue.repository;

import com.sportvenue.entity.RefundExceptionRequest;
import com.sportvenue.entity.enums.RefundExceptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundExceptionRepository extends JpaRepository<RefundExceptionRequest, Integer> {

    /** Lấy yêu cầu theo bookingId (có thể nhiều nếu request cũ đã EXPIRED/REJECTED). */
    List<RefundExceptionRequest> findByBookingBookingId(Integer bookingId);

    /**
     * Kiểm tra booking đã có request đang trong trạng thái xử lý chưa — chặn tạo trùng.
     * Các trạng thái "đang mở": PENDING_OWNER, PENDING_ADMIN, APPROVED_OWNER, APPROVED_ADMIN.
     */
    boolean existsByBookingBookingIdAndStatusIn(Integer bookingId, List<RefundExceptionStatus> statuses);

    /**
     * Danh sách tất cả request theo nhiều status — Admin xem hàng đợi.
     * JOIN FETCH booking/stadium/customer để tránh N+1 khi toResponse() truy cập các quan hệ
     * LAZY này cho từng dòng trong page — ManyToOne nên phân trang LIMIT/OFFSET ở DB vẫn đúng,
     * không rơi vào cảnh báo in-memory pagination (chỉ OneToMany/ManyToMany mới bị).
     */
    @Query(value = """
            SELECT r FROM RefundExceptionRequest r
            JOIN FETCH r.booking b
            JOIN FETCH b.stadium
            JOIN FETCH r.customer
            WHERE r.status IN :statuses
            """,
            countQuery = "SELECT COUNT(r) FROM RefundExceptionRequest r WHERE r.status IN :statuses")
    Page<RefundExceptionRequest> findAllByStatusIn(@Param("statuses") List<RefundExceptionStatus> statuses, Pageable pageable);

    /**
     * Danh sách request của 1 khách hàng — phân trang. JOIN FETCH cùng lý do như trên.
     * Giữ nguyên {@code ORDER BY createdAt DESC} tường minh — chuyển từ derived query sang
     * {@code @Query} sẽ mất sort ngầm định mã hoá trong tên method gốc nếu không khai báo lại.
     */
    @Query(value = """
            SELECT r FROM RefundExceptionRequest r
            JOIN FETCH r.booking b
            JOIN FETCH b.stadium
            JOIN FETCH r.customer
            WHERE r.customer.userId = :userId
            ORDER BY r.createdAt DESC
            """,
            countQuery = "SELECT COUNT(r) FROM RefundExceptionRequest r WHERE r.customer.userId = :userId")
    Page<RefundExceptionRequest> findByCustomerUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId, Pageable pageable);

    /**
     * Lấy request PENDING_OWNER tạo trước một mốc thời gian — dùng cho SLA escalation scheduler.
     * Đánh index trên (status, created_at) tại V105 đảm bảo query nhanh.
     */
    @Query("SELECT r FROM RefundExceptionRequest r WHERE r.status = :status AND r.createdAt < :deadline")
    List<RefundExceptionRequest> findByStatusAndCreatedAtBefore(
            @Param("status") RefundExceptionStatus status,
            @Param("deadline") LocalDateTime deadline);

    /**
     * Danh sách PENDING_OWNER cho đúng sân của Owner — trang "Yêu cầu ngoại lệ đang chờ".
     * Join qua booking → stadium → owner để lọc đúng sân của Owner đang đăng nhập. JOIN FETCH
     * booking/stadium/customer để tránh N+1 khi toResponse() truy cập các quan hệ LAZY này —
     * JOIN s.owner giữ nguyên (không FETCH) vì chỉ dùng để lọc, không cần trả về owner.
     */
    @Query(value = """
            SELECT r FROM RefundExceptionRequest r
            JOIN FETCH r.booking b
            JOIN FETCH b.stadium s
            JOIN FETCH r.customer
            JOIN s.owner o
            WHERE o.user.email = :ownerEmail
              AND r.status IN :statuses
            ORDER BY r.createdAt ASC
            """,
            countQuery = """
            SELECT COUNT(r) FROM RefundExceptionRequest r
            JOIN r.booking b
            JOIN b.stadium s
            JOIN s.owner o
            WHERE o.user.email = :ownerEmail
              AND r.status IN :statuses
            """)
    Page<RefundExceptionRequest> findByOwnerEmailAndStatusIn(
            @Param("ownerEmail") String ownerEmail,
            @Param("statuses") List<RefundExceptionStatus> statuses,
            Pageable pageable);

    /**
     * Lấy TẤT CẢ request của 1 booking, mới nhất trước (để hiển thị badge trạng thái trên UI —
     * caller lấy phần tử đầu qua {@code .stream().findFirst()}). Trả về List thay vì
     * {@code Optional<RefundExceptionRequest>} vì 1 booking có thể có NHIỀU request theo thời
     * gian (vd: request cũ đã EXPIRED/REJECTED_ADMIN rồi khách nộp lại request mới) — dùng
     * {@code Optional} trực tiếp ở đây sẽ ném {@code NonUniqueResultException} khi có ≥2 dòng.
     */
    @Query("""
            SELECT r FROM RefundExceptionRequest r
            WHERE r.booking.bookingId = :bookingId
            ORDER BY r.createdAt DESC
            """)
    List<RefundExceptionRequest> findLatestByBookingId(@Param("bookingId") Integer bookingId);
}
