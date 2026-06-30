package com.sportvenue.repository;

import com.sportvenue.entity.ChatbotLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatbotLogRepository extends JpaRepository<ChatbotLog, Long> {

    /**
     * Get chatbot conversation history for a user, newest first.
     */
    Page<ChatbotLog> findByUserUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
