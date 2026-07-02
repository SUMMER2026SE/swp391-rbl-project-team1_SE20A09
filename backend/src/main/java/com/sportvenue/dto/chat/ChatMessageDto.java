package com.sportvenue.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for a single chat message — used in both REST responses and WebSocket payloads.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    private Long messageId;
    private Long conversationId;
    private Integer senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String messageType;
    private boolean isRead;
    private LocalDateTime sentAt;
    
    // Legacy fields for UI compatibility
    private String quotedMessageContent;
    private boolean forwarded;
    private Object reactions;
    
    // Seen-by indicator
    private java.util.List<String> readByAvatars;
    private java.util.List<String> readByNames;
}
