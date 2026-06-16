package com.sportvenue.dto.response;

import com.sportvenue.entity.enums.JoinRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinRequestResponse {
    private Integer joinId;
    private Integer matchId;
    private Integer userId;
    private String fullName;
    private String email;
    private JoinRequestStatus requestStatus;
    private String message;
    private LocalDateTime createdAt;
}
