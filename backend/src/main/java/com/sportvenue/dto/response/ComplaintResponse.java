package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponse {

    private String id;
    private Integer complaintId;
    private String subject;
    private String against;
    private String description;
    private String status;
    private String priority;
    private String submittedDate;
    private String resolvedDate;
    private String resolution;
    private Integer bookingId;
    private String customerName;
    private String customerEmail;
    private String stadiumName;
    private Integer stadiumId;
    private String ownerName;
    private String ownerEmail;
    private String bookingStatus;
    private List<ResponseItem> responses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseItem {
        private String from;
        private String message;
        private String time;
    }
}
