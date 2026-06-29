package com.sportvenue.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for sending a new chat message (both REST and WebSocket).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    // Either recipientId (for 1-on-1) or conversationId (for group/existing) must be provided
    private Integer recipientId;

    private Long conversationId;

    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String content;

    /** Optional: defaults to TEXT if not provided. */
    private String messageType;
}
