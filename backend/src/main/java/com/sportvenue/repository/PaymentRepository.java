package com.sportvenue.repository;

import com.sportvenue.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho Payment entity.
 * Stub — mở rộng thêm khi implement luồng thanh toán.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    /** Tìm payment theo booking — dùng để kiểm tra trạng thái thanh toán. */
    Optional<Payment> findByBookingBookingId(Integer bookingId);

    /** Tìm payment theo mã giao dịch — dùng khi xử lý callback từ VNPay/MoMo. */
    Optional<Payment> findByTransactionCode(String transactionCode);
}
