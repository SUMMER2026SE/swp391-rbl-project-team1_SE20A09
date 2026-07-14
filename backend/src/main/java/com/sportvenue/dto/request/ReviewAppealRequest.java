package com.sportvenue.dto.request;

import com.sportvenue.entity.enums.AppealStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewAppealRequest {

    @NotNull(message = "Trạng thái xử lý không được để trống")
    private AppealStatus status;

    @Size(max = 2000, message = "Ghi chú Admin không được vượt quá 2000 ký tự")
    private String adminNote;
}
