package com.sportvenue.entity;

import com.sportvenue.entity.enums.JoinRequestStatus;
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
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng join_requests.
 * Đại diện cho yêu cầu xin tham gia kèo ghép của người dùng khác.
 */
@Entity
@Table(name = "join_requests", indexes = {
        @Index(name = "idx_join_requests_match_id", columnList = "match_id"),
        @Index(name = "idx_join_requests_user_id", columnList = "user_id"),
        @Index(name = "idx_join_requests_status", columnList = "request_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "join_id")
    private Integer joinId;

    /** Trận đấu ghép mà người dùng xin tham gia */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchRequest matchRequest;

    /** Người dùng gửi yêu cầu xin tham gia */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false, length = 20)
    @Builder.Default
    private JoinRequestStatus requestStatus = JoinRequestStatus.PENDING;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
