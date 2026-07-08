package com.sportvenue.repository;

import com.sportvenue.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    /**
     * Find the conversation between two users.
     * Caller must ensure u1 < u2 for canonical ordering.
     */
    @Query("SELECT c FROM ChatConversation c " +
           "WHERE c.isGroup = false AND c.user1.userId = :u1 AND c.user2.userId = :u2")
    Optional<ChatConversation> findByUserPair(@Param("u1") Integer u1, @Param("u2") Integer u2);
    
    /**
     * Find the group conversation associated with a specific match.
     */
    @Query("SELECT c FROM ChatConversation c WHERE c.match.matchId = :matchId")
    Optional<ChatConversation> findByMatchId(@Param("matchId") Integer matchId);

    /**
     * Get all conversations for a user, ordered by most recent message.
     * Eagerly fetches both user references to avoid N+1.
     */
    @Query("SELECT DISTINCT c FROM ChatConversation c " +
           "LEFT JOIN FETCH c.user1 " +
           "LEFT JOIN FETCH c.user2 " +
           "LEFT JOIN FETCH c.participants p " +
           "LEFT JOIN ChatConversationHiddenState h ON h.conversationId = c.conversationId AND h.userId = :userId " +
           "WHERE ((c.isGroup = false AND (c.user1.userId = :userId OR c.user2.userId = :userId)) " +
           "       OR (c.isGroup = true AND p.userId = :userId)) " +
           "AND (h IS NULL OR c.lastMessageAt > h.hiddenAt) " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<ChatConversation> findAllByUserId(@Param("userId") Integer userId);
}
