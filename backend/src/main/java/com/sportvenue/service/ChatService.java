package com.sportvenue.service;

import com.sportvenue.dto.chat.ChatMessageDto;
import com.sportvenue.dto.chat.ChatbotRequest;
import com.sportvenue.dto.chat.ChatbotResponse;
import com.sportvenue.dto.chat.ConversationDto;
import com.sportvenue.dto.chat.SendMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for real-time chat and AI chatbot functionality.
 */
public interface ChatService {

    /**
     * Get all conversations for the current user, with unread counts.
     */
    List<ConversationDto> getConversations(Integer userId);

    /**
     * Get paginated message history for a conversation.
     * Validates that the requesting user is a participant.
     */
    Page<ChatMessageDto> getMessages(Long conversationId, Integer userId, Pageable pageable);

    /**
     * Send a message from sender to recipient.
     * Creates the conversation if it doesn't exist yet.
     * Returns the saved message DTO.
     */
    ChatMessageDto sendMessage(Integer senderId, SendMessageRequest request);

    /**
     * Mark all messages in a conversation as read for the given user.
     */
    void markAsRead(Long conversationId, Integer userId);

    /**
     * Get total unread message count for a user.
     */
    long getUnreadCount(Integer userId);

    /**
     * Process a chatbot query and return AI-generated response.
     */
    ChatbotResponse processChatbotQuery(Integer userId, ChatbotRequest request);

    /**
     * Get or create a conversation between two users.
     * Returns the conversation ID.
     */
    Long getOrCreateConversation(Integer userId1, Integer userId2);

    /**
     * Search users by name for starting new conversations.
     */
    List<ConversationDto> searchUsers(Integer currentUserId, String query);

    /**
     * Create or update a group chat for a match and add a new user.
     */
    void createOrUpdateMatchGroupChat(com.sportvenue.entity.MatchRequest match, Integer newUserId);
}
