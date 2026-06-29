package com.sportvenue.controller;

import com.sportvenue.dto.chat.ChatMessageDto;
import com.sportvenue.dto.chat.ChatbotRequest;
import com.sportvenue.dto.chat.ChatbotResponse;
import com.sportvenue.dto.chat.ConversationDto;
import com.sportvenue.dto.chat.SendMessageRequest;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Real-time chat & chatbot APIs")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Conversations ────────────────────────────────────────────

    @GetMapping("/conversations")
    @Operation(summary = "Get all conversations for current user")
    public ResponseEntity<List<ConversationDto>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ConversationDto> conversations = chatService.getConversations(principal.getUserId());
        return ResponseEntity.ok(conversations);
    }

    @PostMapping("/conversations/{recipientId}")
    @Operation(summary = "Get or create a conversation with another user")
    public ResponseEntity<Map<String, Long>> getOrCreateConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer recipientId) {
        Long conversationId = chatService.getOrCreateConversation(
                principal.getUserId(), recipientId);
        return ResponseEntity.ok(Map.of("conversationId", conversationId));
    }

    // ── Messages ─────────────────────────────────────────────────

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Get message history for a conversation (paginated)")
    public ResponseEntity<Page<ChatMessageDto>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Page<ChatMessageDto> messages = chatService.getMessages(
                conversationId,
                principal.getUserId(),
                PageRequest.of(page, size, Sort.by("sentAt").descending())
        );
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a chat message")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendMessageRequest request) {

        ChatMessageDto message = chatService.sendMessage(principal.getUserId(), request);

        // Broadcast via WebSocket to the recipient's personal topic
        messagingTemplate.convertAndSend(
                "/topic/chat/user/" + request.getRecipientId(),
                message
        );
        // Also send to sender's topic (for multi-tab sync)
        messagingTemplate.convertAndSend(
                "/topic/chat/user/" + principal.getUserId(),
                message
        );

        log.info("Chat message sent and broadcast: {} -> {}", principal.getUserId(), request.getRecipientId());
        return ResponseEntity.ok(message);
    }

    // ── Read receipts ────────────────────────────────────────────

    @PostMapping("/conversations/{conversationId}/read")
    @Operation(summary = "Mark all messages in a conversation as read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long conversationId) {
        chatService.markAsRead(conversationId, principal.getUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get total unread message count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        long count = chatService.getUnreadCount(principal.getUserId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ── User search ──────────────────────────────────────────────

    @GetMapping("/users/search")
    @Operation(summary = "Search users to start a new conversation")
    public ResponseEntity<List<ConversationDto>> searchUsers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String q) {
        List<ConversationDto> results = chatService.searchUsers(principal.getUserId(), q);
        return ResponseEntity.ok(results);
    }

    // ── Chatbot ──────────────────────────────────────────────────

    @PostMapping("/chatbot")
    @Operation(summary = "Send a query to the AI chatbot")
    public ResponseEntity<ChatbotResponse> chatbot(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChatbotRequest request) {
        ChatbotResponse response = chatService.processChatbotQuery(
                principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
