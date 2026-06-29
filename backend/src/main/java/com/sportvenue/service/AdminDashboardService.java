package com.sportvenue.service;

import com.sportvenue.dto.response.AdminDashboardResponse;

import java.time.LocalDate;

public interface AdminDashboardService {

    AdminDashboardResponse getDashboardData();

    AdminDashboardResponse getDashboardData(LocalDate startDate, LocalDate endDate);
}
