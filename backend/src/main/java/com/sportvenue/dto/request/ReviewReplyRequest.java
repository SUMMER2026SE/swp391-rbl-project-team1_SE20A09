package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO cho Owner phản hồi đánh giá của khách hàng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyRequest {

    /** Nội dung phản hồi của Owner. */
    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(max = 1000, message = "Nội dung phản hồi không được vượt quá 1000 ký tự")
    private String ownerResponse;
}
