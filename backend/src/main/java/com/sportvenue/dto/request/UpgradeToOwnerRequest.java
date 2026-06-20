package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpgradeToOwnerRequest {

    @NotBlank(message = "Tên thương hiệu/doanh nghiệp không được để trống")
    @Size(max = 100, message = "Tên doanh nghiệp không được vượt quá 100 ký tự")
    private String businessName;

    @NotBlank(message = "Mã số thuế không được để trống")
    @Pattern(
            regexp = "^(?:\\d{10}|\\d{13}|\\d{10}-\\d{3})$",
            message = "Mã số thuế không hợp lệ (phải gồm 10 hoặc 13 chữ số)"
    )
    @Size(max = 30, message = "Mã số thuế không được vượt quá 30 ký tự")
    private String taxCode;

    @NotBlank(message = "Địa chỉ kinh doanh không được để trống")
    private String businessAddress;
}
