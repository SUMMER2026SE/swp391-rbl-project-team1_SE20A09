package com.sportvenue.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for sending a chatbot query.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String message;
}
