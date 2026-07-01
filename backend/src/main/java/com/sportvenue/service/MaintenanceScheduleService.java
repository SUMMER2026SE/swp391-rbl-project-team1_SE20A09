package com.sportvenue.service;

import com.sportvenue.dto.request.CreateMaintenanceScheduleRequest;
import com.sportvenue.dto.response.MaintenanceScheduleResponse;
import com.sportvenue.entity.Stadium;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceScheduleService {

    /**
     * Single source of truth cho "sân có đang bảo trì vào ngày {@code date} không?".
     * Kiểm tra cả {@code stadium.stadiumStatus} (bảo trì vô thời hạn, cascade từ FACILITY
     * cha nếu {@code stadium} là COURT) lẫn các {@link com.sportvenue.entity.MaintenanceSchedule}
     * đang active (bảo trì có khung ngày).
     */
    boolean isStadiumUnderMaintenance(Stadium stadium, LocalDate date);

    MaintenanceScheduleResponse createSchedule(Integer stadiumId, CreateMaintenanceScheduleRequest request, Integer userId);

    /** Bảo trì có khung ngày ở cấp Complex — cascade xuống toàn bộ Facility + Court con. */
    MaintenanceScheduleResponse createComplexSchedule(Integer complexId, CreateMaintenanceScheduleRequest request, Integer userId);

    /** Dùng chung cho cả khung gắn ở Stadium lẫn Complex — tự phân giải theo {@code schedule.getStadium()}/{@code getComplex()}. */
    void endSchedule(Integer maintenanceId, Integer userId);

    List<MaintenanceScheduleResponse> listSchedules(Integer stadiumId);

    List<MaintenanceScheduleResponse> listComplexSchedules(Integer complexId);
}
