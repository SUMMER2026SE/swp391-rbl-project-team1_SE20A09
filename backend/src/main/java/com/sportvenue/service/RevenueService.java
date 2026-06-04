package com.sportvenue.service;

import com.sportvenue.dto.response.RevenueReportResponse;
import java.time.LocalDateTime;

public interface RevenueService {
    RevenueReportResponse getRevenueReport(String ownerEmail, Integer stadiumId, LocalDateTime startDate, LocalDateTime endDate);
}
