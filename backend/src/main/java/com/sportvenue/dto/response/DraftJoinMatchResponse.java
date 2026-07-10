package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftJoinMatchResponse {
    private Integer matchId;
    private String title;
    private String stadiumName;
    private String playDate;
    private String time;
    private String userMessage;
}
