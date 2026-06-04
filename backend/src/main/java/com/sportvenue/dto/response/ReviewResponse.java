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
public class ReviewResponse {
    private Integer reviewId;
    private String id;
    private Integer bookingId;
    private String venueName;
    private String stadiumName;
    private String customerName;
    private Integer rating;
    private String comment;
    private List<String> tags;
    private String createdAt;
    private String ownerResponse;
}
