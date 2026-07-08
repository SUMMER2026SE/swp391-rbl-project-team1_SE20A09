package com.sportvenue.repository;

import com.sportvenue.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {
}
