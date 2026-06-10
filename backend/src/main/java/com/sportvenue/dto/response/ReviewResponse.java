package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Integer reviewId;
    private Integer bookingId;
    private Integer stadiumId;
    private String reviewerName;
    private Integer ratingScore;
    private String comment;
    private String ownerResponse;
    private LocalDateTime createdAt;
}
