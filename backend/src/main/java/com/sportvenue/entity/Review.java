package com.sportvenue.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng reviews.
 * Đánh giá của khách hàng sau khi sử dụng sân — chỉ được tạo sau khi booking COMPLETED.
 */
@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_stadium_id", columnList = "stadium_id"),
        @Index(name = "idx_reviews_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    /** Mỗi booking chỉ được review một lần (UNIQUE constraint). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /** Người dùng viết review. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Sân được đánh giá — để query nhanh mà không cần join qua booking. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    /** Điểm đánh giá từ 1 đến 5. */
    @Column(name = "rating_score", nullable = false)
    private Integer ratingScore;

    /** Nhận xét chi tiết của khách hàng. */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /** Phản hồi của Owner đối với review này. */
    @Column(name = "owner_response", columnDefinition = "TEXT")
    private String ownerResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
