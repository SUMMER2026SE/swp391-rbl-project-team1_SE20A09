package com.sportvenue.repository;

import com.sportvenue.entity.ChatConversationHiddenState;
import com.sportvenue.entity.ChatConversationHiddenStateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatConversationHiddenStateRepository extends JpaRepository<ChatConversationHiddenState, ChatConversationHiddenStateId> {
    Optional<ChatConversationHiddenState> findByConversationIdAndUserId(Long conversationId, Integer userId);
}
