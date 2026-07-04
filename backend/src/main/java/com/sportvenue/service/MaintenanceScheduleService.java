package com.sportvenue.service;

import com.sportvenue.dto.request.CreateMaintenanceScheduleRequest;
import com.sportvenue.dto.response.MaintenanceScheduleResponse;
import com.sportvenue.entity.MaintenanceSchedule;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface MaintenanceScheduleService {

    /**
     * Single source of truth cho "sân có đang bảo trì vào ngày {@code date} không?" — check ở mức
     * NGÀY, không phân biệt khung giờ (VD: bảo trì chỉ 10h-12h vẫn coi cả ngày là "có bảo trì").
     * Kiểm tra cả {@code stadium.stadiumStatus} (bảo trì vô thời hạn, cascade từ FACILITY
     * cha nếu {@code stadium} là COURT) lẫn các {@link com.sportvenue.entity.MaintenanceSchedule}
     * đang active. Dùng cho các chỗ chỉ cần biết "ngày này có đụng bảo trì không, bất kể giờ nào"
     * (match request) — KHÔNG dùng để chặn 1 slot cụ thể (xem {@link #isSlotUnderMaintenance}) hoặc
     * hiển thị badge "đang bảo trì ngay bây giờ" (xem {@link #isStadiumUnderMaintenanceNow}).
     */
    boolean isStadiumUnderMaintenance(Stadium stadium, LocalDate date);

    /**
     * Biến thể chính xác theo khung giờ của {@link #isStadiumUnderMaintenance} — chỉ trả về true
     * nếu khoảng [slotStart, slotEnd) của slot thực sự chồng lấn với khung giờ hiệu lực của lịch
     * bảo trì (hoặc bảo trì cả ngày). Dùng khi tạo booking — nơi cần biết đúng 1 slot có bị chặn
     * hay không, thay vì cả ngày.
     */
    boolean isSlotUnderMaintenance(Stadium stadium, LocalDate date, LocalTime slotStart, LocalTime slotEnd);

    /**
     * Biến thể cho cấp Complex — dùng khi chưa xác định được Court/Facility cụ thể
     * (VD: match request chỉ chọn Complex chung chung, chưa chọn sân).
     */
    boolean isComplexUnderMaintenance(StadiumComplex complex, LocalDate date);

    /**
     * "Sân có đang bảo trì NGAY LÚC NÀY không?" — check theo đúng thời điểm hiện tại
     * (không phải chỉ "ngày hôm nay có đụng bảo trì"). Dùng cho badge "Đang bảo trì" hiển thị
     * công khai — lịch bảo trì sắp tới (VD 20h-22h tối nay) mà bây giờ mới 10h sáng thì KHÔNG
     * được coi là "đang bảo trì". Xem {@link #isStadiumUnderMaintenance} cho check mức ngày.
     */
    boolean isStadiumUnderMaintenanceNow(Stadium stadium);

    /**
     * Batch version của {@link #isStadiumUnderMaintenanceNow} cho nhiều sân —
     * tối đa 2 query bất kể danh sách dài bao nhiêu (thay vì N lần gọi lặp gây N+1).
     * Dùng cho danh sách sân của Owner và danh sách Facility/Court công khai.
     */
    Map<Integer, Boolean> isUnderMaintenanceNow(List<Stadium> stadiums);

    /** Bảo trì trong 1 ngày cụ thể — {@code allDay} = true nếu bị chặn cả ngày (không cần xét giờ). */
    record DayMaintenance(boolean allDay, List<MaintenanceSchedule> schedules) {

        public static final DayMaintenance NONE = new DayMaintenance(false, List.of());
        public static final DayMaintenance ALL_DAY = new DayMaintenance(true, List.of());

        /** True nếu slot [slotStart, slotEnd) trong ngày {@code date} chồng lấn 1 trong các khung bảo trì. */
        public boolean overlaps(LocalTime slotStart, LocalTime slotEnd, LocalDate date) {
            if (allDay) {
                return true;
            }
            LocalDateTime rangeStart = LocalDateTime.of(date, slotStart);
            LocalDateTime rangeEnd = LocalDateTime.of(date, slotEnd);
            return schedules.stream().anyMatch(s -> s.overlaps(rangeStart, rangeEnd));
        }
    }

    /**
     * Batch version theo khung giờ của {@link #isSlotUnderMaintenance} cho 1 sân trên cả 1 khoảng
     * ngày — tối đa 2 query bất kể khoảng ngày dài bao nhiêu (thay vì gọi lặp mỗi ngày). Dùng cho
     * lịch tuần ({@code getWeeklySlots}) — mỗi slot tự gọi {@link DayMaintenance#overlaps} với giờ
     * riêng của slot đó thay vì chặn nguyên ngày.
     */
    Map<LocalDate, DayMaintenance> getDayMaintenanceForDateRange(Stadium stadium, LocalDate rangeStart, LocalDate rangeEnd);

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
