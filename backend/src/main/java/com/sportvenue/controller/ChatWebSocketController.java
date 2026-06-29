package com.sportvenue.controller;

import com.sportvenue.dto.chat.ChatMessageDto;
import com.sportvenue.dto.chat.SendMessageRequest;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ChatService;
import com.sportvenue.entity.User;
import com.sportvenue.entity.ChatConversation;
import com.sportvenue.repository.ChatConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket STOMP controller for real-time chat messaging.
 * Handles messages sent to /app/chat.send and /app/chat.typing.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatConversationRepository conversationRepo;

    /**
     * Handle incoming chat messages via STOMP.
     * Client sends to: /app/chat.send
     * Server broadcasts to: /topic/chat/user/{recipientId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        UserPrincipal principal = extractPrincipal(headerAccessor);
        if (principal == null) {
            log.warn("WebSocket chat.send: unauthenticated message rejected");
            return;
        }

        ChatMessageDto message = chatService.sendMessage(principal.getUserId(), request);

        ChatConversation conv = conversationRepo.findById(message.getConversationId()).orElse(null);
        if (conv != null) {
            if (Boolean.TRUE.equals(conv.getIsGroup())) {
                for (User participant : conv.getParticipants()) {
                    messagingTemplate.convertAndSend(
                            "/topic/chat/user/" + participant.getUserId(),
                            message
                    );
                }
            } else {
                Integer otherId = (conv.getUser1() != null && conv.getUser1().getUserId().equals(principal.getUserId()))
                        ? conv.getUser2().getUserId()
                        : conv.getUser1().getUserId();
                messagingTemplate.convertAndSend("/topic/chat/user/" + otherId, message);
                messagingTemplate.convertAndSend("/topic/chat/user/" + principal.getUserId(), message);
            }
        }
    }

    /**
     * Handle typing indicator events.
     * Client sends to: /app/chat.typing
     * Server broadcasts to: /topic/chat/typing/{recipientId}
     */
    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload Map<String, Object> payload,
                                SimpMessageHeaderAccessor headerAccessor) {
        UserPrincipal principal = extractPrincipal(headerAccessor);
        if (principal == null) return;

        Integer recipientId = (Integer) payload.get("recipientId");
        Boolean isTyping = (Boolean) payload.getOrDefault("isTyping", false);

        if (recipientId != null) {
            messagingTemplate.convertAndSend(
                    "/topic/chat/typing/" + recipientId,
                    Map.of(
                            "userId", principal.getUserId(),
                            "userName", principal.getUsername(),
                            "isTyping", isTyping
                    )
            );
        }
    }

    private UserPrincipal extractPrincipal(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
            Object principalObj = auth.getPrincipal();
            if (principalObj instanceof UserPrincipal up) {
                return up;
            }
        }
        return null;
    }
}
