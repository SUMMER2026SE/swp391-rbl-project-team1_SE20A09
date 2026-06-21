package com.sportvenue.dto.response;

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
public class ComplaintChatEventDTO {
    private Integer complaintId;
    private String from;
    private String message;
    private String time;
    private String newStatus;
}
