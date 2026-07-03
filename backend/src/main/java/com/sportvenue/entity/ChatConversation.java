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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;

/**
 * Represents a 1-on-1 chat conversation between two users.
 * Uses (user1_id, user2_id) unique constraint where user1_id < user2_id
 * to avoid duplicates (A→B same as B→A).
 */
@Entity
@Table(name = "chat_conversations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_conv_users", columnNames = {"user1_id", "user2_id"})
        },
        indexes = {
                @Index(name = "idx_chat_conv_user1", columnList = "user1_id"),
                @Index(name = "idx_chat_conv_user2", columnList = "user2_id"),
                @Index(name = "idx_chat_conv_updated", columnList = "last_message_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Long conversationId;

    /** The user with the smaller user_id — enforced by application logic. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id")
    private User user1;

    /** The user with the larger user_id — enforced by application logic. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id")
    private User user2;

    @Column(name = "name")
    private String name;

    @Column(name = "user1_nickname", length = 100)
    private String user1Nickname;

    @Column(name = "user2_nickname", length = 100)
    private String user2Nickname;

    @Column(name = "is_group")
    @Builder.Default
    private Boolean isGroup = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private MatchRequest match;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "chat_group_members",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<User> participants = new HashSet<>();

    /** Cached snippet for conversation list. */
    @Column(name = "last_message_preview", length = 255)
    private String lastMessagePreview;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** The user who initiated the block (2-way mutual block). Null means not blocked. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by")
    private User blockedBy;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "is_mutual_block")
    @Builder.Default
    private Boolean isMutualBlock = false;
}
