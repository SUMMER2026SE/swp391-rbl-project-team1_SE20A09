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
           "LEFT JOIN ChatConversationHiddenState h ON h.conversationId = m.conversation.conversationId AND h.userId = :userId " +
           "WHERE m.conversation.conversationId = :conversationId " +
           "AND :userId NOT IN (SELECT hb.userId FROM m.hiddenBy hb) " +
           "AND (h IS NULL OR m.sentAt > h.hiddenAt) " +
           "ORDER BY m.sentAt DESC")
    Page<ChatMessage> findByConversationId(
            @Param("conversationId") Long conversationId,
            @Param("userId") Integer userId,
            Pageable pageable
    );

    /**
     * Count unread messages for a user across all conversations.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "LEFT JOIN m.conversation.participants p " +
           "LEFT JOIN ChatConversationHiddenState h ON h.conversationId = m.conversation.conversationId AND h.userId = :userId " +
           "WHERE ((m.conversation.isGroup = false AND (m.conversation.user1.userId = :userId OR m.conversation.user2.userId = :userId)) " +
           "   OR (m.conversation.isGroup = true AND p.userId = :userId)) " +
           "AND m.sender.userId <> :userId " +
           "AND (h IS NULL OR m.sentAt > h.hiddenAt) " +
           "AND (m.isRead = false AND :userId NOT IN (SELECT r.userId FROM m.readBy r))")
    long countUnreadForUser(@Param("userId") Integer userId);

    /**
     * Count unread messages in a specific conversation for the given recipient.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.conversation.conversationId = :convId " +
           "AND m.sender.userId <> :userId " +
           "AND (m.isRead = false AND :userId NOT IN (SELECT r.userId FROM m.readBy r))")
    long countUnreadInConversation(
            @Param("convId") Long convId,
            @Param("userId") Integer userId
    );

    /**
     * Mark all messages in a 1-on-1 conversation as read.
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true " +
           "WHERE m.conversation.conversationId = :convId " +
           "AND m.sender.userId <> :userId AND m.isRead = false")
    void markAllAsRead(@Param("convId") Long convId, @Param("userId") Integer userId);

    /**
     * Find unread messages for a specific recipient in a conversation.
     * Used for adding the recipient to readBy.
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.conversation.conversationId = :convId " +
           "AND m.sender.userId <> :userId " +
           "AND (m.isRead = false AND :userId NOT IN (SELECT r.userId FROM m.readBy r))")
    java.util.List<ChatMessage> findUnreadMessagesInConversation(
            @Param("convId") Long convId,
            @Param("userId") Integer userId
    );

    void deleteByConversation(com.sportvenue.entity.ChatConversation conversation);
}
