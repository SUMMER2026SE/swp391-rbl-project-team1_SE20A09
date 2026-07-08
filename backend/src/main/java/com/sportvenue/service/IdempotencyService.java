package com.sportvenue.service;

import java.util.Optional;

/**
 * Chống double-submit cho các thao tác không idempotent (đặt sân, tạo thanh toán).
 *
 * Quy trình:
 *   1. Client gửi header {@code X-Idempotency-Key: <uuid>} mỗi lần submit form.
 *   2. {@link #tryAcquire} trả về {@code true} nếu đây là lần gọi đầu tiên → tiếp tục xử lý.
 *   3. Sau khi xử lý xong, gọi {@link #complete} để lưu bookingId kết quả.
 *   4. Các lần gọi lại: {@link #getExistingBookingId} trả về bookingId cũ → trả về response cũ.
 */
public interface IdempotencyService {

    /**
     * Cố chiếm slot idempotency key cho (userId, key).
     *
     * @return {@code true} nếu đây là lần đầu (key chưa tồn tại) — caller phải gọi
     *         {@link #complete} hoặc {@link #release} sau khi xử lý.
     *         {@code false} nếu key đang trong trạng thái PROCESSING (concurrent duplicate).
     */
    boolean tryAcquire(Integer userId, String idempotencyKey);

    /**
     * Lưu kết quả bookingId vào Redis — TTL 24 giờ.
     * Gọi sau khi booking được tạo thành công.
     */
    void complete(Integer userId, String idempotencyKey, Integer bookingId);

    /**
     * Xoá key khỏi Redis khi xử lý thất bại — cho phép client retry với cùng key.
     */
    void release(Integer userId, String idempotencyKey);

    /**
     * Trả về bookingId đã được lưu trước đó (nếu có).
     * Dùng cho lần gọi lại sau khi complete.
     */
    Optional<Integer> getExistingBookingId(Integer userId, String idempotencyKey);
}
