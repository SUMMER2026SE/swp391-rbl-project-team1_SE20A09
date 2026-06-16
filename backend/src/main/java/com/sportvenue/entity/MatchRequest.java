package com.sportvenue.entity;

import com.sportvenue.entity.enums.MatchStatus;
import com.sportvenue.entity.enums.SkillLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity ánh xạ bảng match_requests.
 * Đại diện cho một yêu cầu ghép kèo thể thao.
 */
@Entity
@Table(name = "match_requests", indexes = {
        @Index(name = "idx_match_requests_user_id", columnList = "user_id"),
        @Index(name = "idx_match_requests_stadium_id", columnList = "stadium_id"),
        @Index(name = "idx_match_requests_sport_type", columnList = "sport_type_id"),
        @Index(name = "idx_match_requests_play_date", columnList = "play_date"),
        @Index(name = "idx_match_requests_status", columnList = "match_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Integer matchId;

    /** Người tạo kèo ghép (Host) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Sân nơi diễn ra trận đấu */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    /** Môn thể thao của trận đấu */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_type_id", nullable = false)
    private SportType sportType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "play_date", nullable = false)
    private LocalDate playDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "max_players", nullable = false)
    private Integer maxPlayers;

    @Column(name = "current_players", nullable = false)
    @Builder.Default
    private Integer currentPlayers = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false, length = 20)
    @Builder.Default
    private SkillLevel skillLevel = SkillLevel.INTERMEDIATE;

    @Column(name = "split_price", nullable = false)
    @Builder.Default
    private Boolean splitPrice = false;

    @Column(name = "price_per_player", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pricePerPlayer = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 20)
    @Builder.Default
    private MatchStatus matchStatus = MatchStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "matching_type", nullable = false, length = 30)
    @Builder.Default
    private com.sportvenue.entity.enums.MatchingType matchingType = com.sportvenue.entity.enums.MatchingType.INDIVIDUAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
