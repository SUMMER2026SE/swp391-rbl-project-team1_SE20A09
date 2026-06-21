package com.sportvenue.exception;

/**
 * Danh sách mã lỗi chuẩn của hệ thống.
 */
public enum ErrorCode {
    // Auth Errors
    OTP_EXPIRED(400, "Mã xác thực đã hết hạn"),
    OTP_ALREADY_USED(400, "Mã xác thực đã được sử dụng"),
    OTP_INVALID(400, "Mã xác thực không chính xác"),
    OTP_NOT_FOUND(404, "Không tìm thấy mã xác thực"),
    USER_NOT_FOUND(404, "Không tìm thấy người dùng"),
    USER_NOT_VERIFIED(403, "Tài khoản chưa được xác thực email. Vui lòng kiểm tra hộp thư và nhập mã OTP."),

    // Resource Errors
    NOTIFICATION_NOT_FOUND(404, "Không tìm thấy thông báo"),
    OWNER_PROFILE_NOT_FOUND(404, "Không tìm thấy hồ sơ chủ sân"),
    OWNER_PROFILE_NOT_APPROVED(400, "Hồ sơ chủ sân chưa được phê duyệt"),
    UNAUTHORIZED(403, "Bạn không có quyền thực hiện hành động này"),
    DUPLICATE_RESOURCE(409, "Dữ liệu đã tồn tại trong hệ thống"),
    INTERNAL_SERVER_ERROR(500, "Lỗi hệ thống không mong muốn");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
