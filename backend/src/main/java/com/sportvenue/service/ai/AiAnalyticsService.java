package com.sportvenue.service.ai;

import java.time.LocalDateTime;
import java.util.Map;

public interface AiAnalyticsService {
    
    Map<String, Object> getAnalytics(LocalDateTime startDate, LocalDateTime endDate);
}
