package com.sportvenue.entity.enums;

/**
 * Trạng thái của yêu cầu xét duyệt ngoại lệ hoàn tiền (Mục 1.6 P0).
 *
 * <pre>
 * PENDING_OWNER  → Khách vừa gửi, chờ Owner phản hồi (SLA 48h)
 * APPROVED_OWNER → Owner đồng ý hoàn (50% hoặc 100%), chờ gateway
 * REJECTED_OWNER → Owner từ chối; khách có thể leo thang Admin trong 72h
 * PENDING_ADMIN  → Khách leo thang hoặc Owner không phản hồi trong 48h → auto-escalate
 * APPROVED_ADMIN → Admin quyết định hoàn tiền
 * REJECTED_ADMIN → Admin từ chối — kết thúc luồng
 * EXPIRED        → Quá 72h kể từ lúc tạo mà chưa được xử lý / hết hạn submit
 * </pre>
 */
public enum RefundExceptionStatus {
    PENDING_OWNER,
    APPROVED_OWNER,
    REJECTED_OWNER,
    PENDING_ADMIN,
    APPROVED_ADMIN,
    REJECTED_ADMIN,
    EXPIRED
}
