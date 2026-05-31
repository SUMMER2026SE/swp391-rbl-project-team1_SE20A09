package com.sportvenue.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO trả về thông tin đánh giá của khách hàng.
 * Bao gồm cả phản hồi của Owner (nếu có).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Integer reviewId;
    private Integer bookingId;

    /** Thông tin người đánh giá. */
    private ReviewerInfo reviewer;

    /** Thông tin sân được đánh giá. */
    private StadiumSummary stadium;

    /** Điểm đánh giá từ 1 đến 5. */
    private Integer ratingScore;

    /** Nội dung đánh giá. */
    private String comment;

    /** Phản hồi của Owner — null nếu chưa phản hồi. */
    private String ownerResponse;

    private LocalDateTime createdAt;

    /** Thông tin người viết đánh giá. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewerInfo {
        private Integer userId;
        private String fullName;
        private String avatarUrl;
    }

    /** Thông tin tóm tắt sân. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StadiumSummary {
        private Integer stadiumId;
        private String stadiumName;
    }
}
