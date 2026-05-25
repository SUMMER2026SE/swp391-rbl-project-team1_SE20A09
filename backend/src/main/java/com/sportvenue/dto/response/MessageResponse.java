package com.sportvenue.dto.response;

/**
 * Generic success response chỉ chứa message — dùng cho register (không trả JWT).
 */
public record MessageResponse(String message) {
}
