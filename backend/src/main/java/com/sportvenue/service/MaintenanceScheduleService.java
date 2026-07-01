package com.sportvenue.service;

import com.sportvenue.dto.request.CreateMaintenanceScheduleRequest;
import com.sportvenue.dto.response.MaintenanceScheduleResponse;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MaintenanceScheduleService {

    /**
     * Single source of truth cho "sân có đang bảo trì vào ngày {@code date} không?".
     * Kiểm tra cả {@code stadium.stadiumStatus} (bảo trì vô thời hạn, cascade từ FACILITY
     * cha nếu {@code stadium} là COURT) lẫn các {@link com.sportvenue.entity.MaintenanceSchedule}
     * đang active (bảo trì có khung ngày). Dùng cho lookup 1 sân — 1 ngày (VD: tạo booking).
     */
    boolean isStadiumUnderMaintenance(Stadium stadium, LocalDate date);

    /**
     * Biến thể cho cấp Complex — dùng khi chưa xác định được Court/Facility cụ thể
     * (VD: match request chỉ chọn Complex chung chung, chưa chọn sân).
     */
    boolean isComplexUnderMaintenance(StadiumComplex complex, LocalDate date);

    /**
     * Batch version của {@link #isStadiumUnderMaintenance} cho nhiều sân cùng 1 ngày —
     * tối đa 2 query bất kể danh sách dài bao nhiêu (thay vì N lần gọi lặp gây N+1).
     * Dùng cho danh sách sân của Owner và danh sách Facility/Court công khai.
     */
    Map<Integer, Boolean> isUnderMaintenanceToday(List<Stadium> stadiums, LocalDate date);

    /**
     * Batch version của {@link #isStadiumUnderMaintenance} cho 1 sân trên cả 1 khoảng ngày —
     * tối đa 2 query bất kể khoảng ngày dài bao nhiêu (thay vì gọi lặp mỗi ngày). Dùng cho
     * lịch tuần ({@code getWeeklySlots}).
     */
    Map<LocalDate, Boolean> isUnderMaintenanceForDateRange(Stadium stadium, LocalDate rangeStart, LocalDate rangeEnd);

    MaintenanceScheduleResponse createSchedule(Integer stadiumId, CreateMaintenanceScheduleRequest request, Integer userId);

    /** Bảo trì có khung ngày ở cấp Complex — cascade xuống toàn bộ Facility + Court con. */
    MaintenanceScheduleResponse createComplexSchedule(Integer complexId, CreateMaintenanceScheduleRequest request, Integer userId);

    /** Dùng chung cho cả khung gắn ở Stadium lẫn Complex — tự phân giải theo {@code schedule.getStadium()}/{@code getComplex()}. */
    void endSchedule(Integer maintenanceId, Integer userId);

    /** Chỉ chủ sân mới xem được lịch sử bảo trì của chính sân mình. */
    Page<MaintenanceScheduleResponse> listSchedules(Integer stadiumId, Integer userId, Pageable pageable);

    /** Chỉ chủ tổ hợp mới xem được lịch sử bảo trì của chính tổ hợp mình. */
    Page<MaintenanceScheduleResponse> listComplexSchedules(Integer complexId, Integer userId, Pageable pageable);
}
