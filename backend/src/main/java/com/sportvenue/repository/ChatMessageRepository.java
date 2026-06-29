package com.sportvenue.repository;

import com.sportvenue.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Paginated message history for a conversation, ordered newest-first.
     * Eagerly fetches sender to avoid N+1 in message list rendering.
     */
    @Query("SELECT m FROM ChatMessage m " +
           "JOIN FETCH m.sender " +
           "WHERE m.conversation.conversationId = :conversationId " +
           "ORDER BY m.sentAt DESC")
    Page<ChatMessage> findByConversationId(
            @Param("conversationId") Long conversationId,
            Pageable pageable
    );

    /**
     * Count unread messages for a user across all conversations.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.conversation.conversationId IN (" +
           "  SELECT c.conversationId FROM ChatConversation c " +
           "  WHERE c.user1.userId = :userId OR c.user2.userId = :userId" +
           ") AND m.sender.userId <> :userId AND m.isRead = false")
    long countUnreadForUser(@Param("userId") Integer userId);

    /**
     * Count unread messages in a specific conversation for the given recipient.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.conversation.conversationId = :convId " +
           "AND m.sender.userId <> :userId AND m.isRead = false")
    long countUnreadInConversation(
            @Param("convId") Long convId,
            @Param("userId") Integer userId
    );

    /**
     * Mark all messages in a conversation as read for the given recipient.
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true " +
           "WHERE m.conversation.conversationId = :convId " +
           "AND m.sender.userId <> :userId AND m.isRead = false")
    void markAllAsRead(@Param("convId") Long convId, @Param("userId") Integer userId);
}
