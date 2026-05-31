package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO cho Owner xác nhận hoặc từ chối đơn đặt sân.
 * Khi từ chối bắt buộc phải kèm lý do.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingActionRequest {

    /** Hành động: CONFIRM hoặc REJECT. */
    @NotNull(message = "Hành động không được để trống")
    private Action action;

    /** Lý do từ chối — bắt buộc khi action = REJECT. */
    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    private String reason;

    /**
     * Enum hành động xác nhận/từ chối đơn đặt sân.
     */
    public enum Action {
        CONFIRM,
        REJECT
    }
}
