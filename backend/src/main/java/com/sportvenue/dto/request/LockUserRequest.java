package com.sportvenue.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockUserRequest {

    @NotNull(message = "Trạng thái enabled không được để trống")
    private Boolean enabled;

}
