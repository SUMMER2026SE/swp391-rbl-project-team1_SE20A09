package com.sportvenue.entity.enums;

/**
 * Các loại giao dịch ví nội bộ.
 */
public enum WalletTransactionType {
    /** Doanh thu đặt sân cộng vào ví Owner */
    BOOKING_CREDIT,

    /** Phí dịch vụ cộng vào ví Platform */
    SERVICE_FEE_CREDIT,

    /** Hoàn tiền trừ từ ví Owner */
    REFUND_DEBIT,

    /** Phí dịch vụ hoàn lại trừ từ ví Platform */
    REFUND_FEE_DEBIT,

    /** Khấu trừ phí dịch vụ từ ví Owner (khi thu tiền mặt) */
    SERVICE_FEE_DEBIT,

    /** Nạp tiền vào ví Customer qua VNPay */
    CUSTOMER_TOPUP_CREDIT,

    /** Hoàn tiền huỷ đơn cộng vào ví Customer */
    CUSTOMER_REFUND_CREDIT,

    /** Khách thanh toán đơn đặt sân bằng ví, trừ từ ví Customer */
    CUSTOMER_PAYMENT_DEBIT
}
