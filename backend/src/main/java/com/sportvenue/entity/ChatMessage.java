package com.sportvenue.entity;

import com.sportvenue.entity.enums.MessageType;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single chat message within a conversation.
 * Supports TEXT, IMAGE, and SYSTEM message types.
 */
@Entity
@Table(name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_msg_conv_time", columnList = "conversation_id, sent_at DESC"),
                @Index(name = "idx_chat_msg_sender", columnList = "sender_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    /** Whether the recipient has read this message (legacy for 1-on-1). */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "chat_message_read_by",
        joinColumns = @JoinColumn(name = "message_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private java.util.Set<User> readBy = new java.util.HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "chat_message_hidden_by",
        joinColumns = @JoinColumn(name = "message_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private java.util.Set<User> hiddenBy = new java.util.HashSet<>();

    @Column(name = "sent_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();
}
