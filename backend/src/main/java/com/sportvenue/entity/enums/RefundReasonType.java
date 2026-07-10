package com.sportvenue.entity.enums;

public enum RefundReasonType {
    CUSTOMER_REQUEST,
    OWNER_FAULT,
    /** Hoàn tiền ngoại lệ do bất khả kháng — được Admin/Owner xét duyệt thủ công. */
    FORCE_MAJEURE
}

