package com.sportvenue.service;

import com.sportvenue.dto.request.CreateReportRequest;
import com.sportvenue.dto.request.ResolveReportRequest;
import com.sportvenue.dto.response.ReportResponse;
import com.sportvenue.entity.enums.ReportCategory;
import com.sportvenue.entity.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    ReportResponse createReport(CreateReportRequest request, String reporterEmail);

    Page<ReportResponse> getMyReports(String reporterEmail, Pageable pageable);

    ReportResponse getMyReport(Integer reportId, String reporterEmail);

    Page<ReportResponse> getAdminReports(ReportStatus status, ReportCategory category, Pageable pageable);

    ReportResponse getAdminReport(Integer reportId);

    ReportResponse updateReportStatus(Integer reportId, ResolveReportRequest request, String adminEmail);
}
