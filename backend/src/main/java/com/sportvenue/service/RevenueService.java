package com.sportvenue.service;

import com.sportvenue.dto.response.RevenueReportResponse;
import com.sportvenue.dto.response.OwnerDashboardSummaryResponse;
import java.time.LocalDateTime;

public interface RevenueService {
    RevenueReportResponse getRevenueReport(String ownerEmail, Integer stadiumId, LocalDateTime startDate, LocalDateTime endDate);
    OwnerDashboardSummaryResponse getDashboardSummary(String ownerEmail);
}

