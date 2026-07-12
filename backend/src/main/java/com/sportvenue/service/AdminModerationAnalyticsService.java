package com.sportvenue.service;

import com.sportvenue.dto.response.AdminModerationAnalyticsResponse;

import java.time.LocalDate;

public interface AdminModerationAnalyticsService {

    AdminModerationAnalyticsResponse getAnalytics(
            LocalDate from,
            LocalDate to,
            String role,
            String category,
            String status,
            int topLimit);
}
