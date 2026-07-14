package com.sportvenue.service;

import com.sportvenue.dto.response.AdminModerationAnalyticsResponse;
import com.sportvenue.entity.enums.ComplaintPriority;
import com.sportvenue.entity.enums.ComplaintStatus;
import com.sportvenue.entity.enums.ReportCategory;
import com.sportvenue.entity.enums.ReportStatus;

import java.time.LocalDate;

public interface AdminModerationAnalyticsService {

    AdminModerationAnalyticsResponse getAnalytics(
            LocalDate from,
            LocalDate to,
            String role,
            ReportCategory reportCategory,
            ComplaintPriority complaintPriority,
            ReportStatus reportStatus,
            ComplaintStatus complaintStatus,
            int topLimit);
}
