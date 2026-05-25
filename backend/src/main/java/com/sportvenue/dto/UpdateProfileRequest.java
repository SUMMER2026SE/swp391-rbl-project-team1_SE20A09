package com.sportvenue.dto;

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
public class UpdateProfileRequest {

    @NotBlank(message = "Tên không được để trống")
    @Size(max = 50, message = "Tên không được quá 50 ký tự")
    private String firstName;

    @Size(max = 50, message = "Họ không được quá 50 ký tự")
    private String lastName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^(0[35789]\\d{8}|\\+84[35789]\\d{8})$",
            message = "Số điện thoại không đúng định dạng Việt Nam"
    )
    private String phoneNumber;

    @Size(max = 255, message = "URL ảnh đại diện không được quá 255 ký tự")
    private String avatarUrl;
}
