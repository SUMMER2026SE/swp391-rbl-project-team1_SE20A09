package com.sportvenue.dto.response;

import com.sportvenue.entity.enums.JoinRequestStatus;
import com.sportvenue.entity.enums.MatchStatus;
import com.sportvenue.entity.enums.MatchingType;
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

    // Rich match details for user sidebar/history
    private String matchTitle;
    private String stadiumName;
    private String complexName;
    private String sportName;
    private java.time.LocalDate playDate;
    private java.time.LocalTime startTime;
    private java.time.LocalTime endTime;
    private String hostName;
    private String hostEmail;
    private Integer hostUserId;
    private MatchStatus matchStatus;
    private MatchingType matchingType;
    private String cancelReason;
}
