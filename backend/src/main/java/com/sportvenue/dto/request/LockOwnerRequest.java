package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class LockOwnerRequest {

    @NotNull(message = "Trạng thái enabled không được để trống")
    private Boolean enabled;

    private String reason;
}
