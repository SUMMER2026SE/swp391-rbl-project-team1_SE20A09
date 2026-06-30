package com.sportvenue.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Conversation summary shown in the sidebar conversation list.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDto {

    private Long conversationId;
    
    private Boolean isGroup;

    /** The "other" participant (not the current user). */
    private Integer otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private boolean otherUserOnline;

    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    /** True if the current user blocked the other user in this conversation */
    private boolean blocked;
    /** True if the other user blocked the current user */
    private boolean blockedByThem;
    /** The userId of whoever initiated the block (null if not blocked) */
    private Integer blockedByUserId;
}
