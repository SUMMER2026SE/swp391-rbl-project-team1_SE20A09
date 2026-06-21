package com.sportvenue.dto.request;

import com.sportvenue.validation.StrongPassword;
import jakarta.validation.constraints.Email;
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
public class RegisterOwnerRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại phải có 10-11 chữ số và bắt đầu bằng số 0")
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @StrongPassword
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

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

    @NotBlank(message = "Giấy phép đăng ký kinh doanh không được để trống")
    private String businessLicenseUrl;

    @NotBlank(message = "Ảnh CCCD/CMND không được để trống")
    private String identityCardUrl;
}
