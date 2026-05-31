package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO cho Owner phản hồi khiếu nại của khách hàng.
 * Khi phản hồi, trạng thái complaint sẽ chuyển sang RESOLVED.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintReplyRequest {

    /** Nội dung phản hồi/giải quyết khiếu nại. */
    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(max = 2000, message = "Nội dung phản hồi không được vượt quá 2000 ký tự")
    private String response;
}
