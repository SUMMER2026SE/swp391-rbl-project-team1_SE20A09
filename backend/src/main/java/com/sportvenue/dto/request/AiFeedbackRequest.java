package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiFeedbackRequest {
    @NotBlank(message = "Message ID is required")
    private String messageId;

    @NotBlank(message = "Rating is required")
    private String rating;
}
