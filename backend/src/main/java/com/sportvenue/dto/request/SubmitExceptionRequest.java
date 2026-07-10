package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body khi khách hàng gửi "Yêu cầu xét duyệt ngoại lệ hoàn tiền".
 */
@Getter
@Setter
public class SubmitExceptionRequest {

    @NotNull(message = "bookingId không được để trống")
    private Integer bookingId;

    @NotBlank(message = "Lý do không được để trống")
    @Size(min = 5, max = 2000, message = "Lý do phải từ 5 đến 2000 ký tự")
    private String reason;

    /** URL bằng chứng đính kèm — tuỳ chọn (ảnh giấy nhập viện, v.v.). */
    @Size(max = 500, message = "evidenceUrl tối đa 500 ký tự")
    private String evidenceUrl;
}
