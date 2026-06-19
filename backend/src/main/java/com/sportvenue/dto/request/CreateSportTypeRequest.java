package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSportTypeRequest {

    @NotBlank(message = "Tên loại môn thể thao không được để trống")
    @Size(max = 50, message = "Tên loại môn thể thao không được vượt quá 50 ký tự")
    private String sportName;

    @Size(max = 50, message = "Tên tiếng Anh không được vượt quá 50 ký tự")
    private String nameEn;


    @NotBlank(message = "Mã môn thể thao không được để trống")
    @Size(max = 20, message = "Mã môn thể thao không được vượt quá 20 ký tự")
    private String sportCode;

    private String description;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isFootballType = false;
}
