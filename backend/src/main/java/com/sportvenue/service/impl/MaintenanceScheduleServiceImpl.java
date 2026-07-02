package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateMaintenanceScheduleRequest;
import com.sportvenue.dto.response.MaintenanceScheduleResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.MaintenanceSchedule;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.ComplexStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.mapper.MaintenanceScheduleMapper;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.MaintenanceScheduleRepository;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.service.MaintenanceScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * UC-OWN: Quản lý lịch bảo trì theo khung ngày ({@link MaintenanceSchedule}) — gắn được ở
 * cấp Stadium (Facility/Court) hoặc cấp Complex, tách biệt khỏi các cờ tĩnh
 * {@code stadium.stadiumStatus}/{@code complex.complexStatus} (bảo trì vô thời hạn).
 * Cả các cơ chế được hợp nhất qua {@link #isStadiumUnderMaintenance(Stadium, LocalDate)}
 * — single source of truth mà booking flow dựa vào.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceScheduleServiceImpl implements MaintenanceScheduleService {

    /** Chỉ booking CONFIRMED mới chặn tạo lịch bảo trì — theo quyết định đã chọn. */
    private static final List<BookingStatus> CONFLICT_STATUSES = List.of(BookingStatus.CONFIRMED);

    /** Cận trên hợp lệ cho cột kiểu DATE của PostgreSQL — dùng thay cho LocalDate.MAX khi endDate = null. */
    private static final LocalDate FAR_FUTURE = LocalDate.of(9999, 12, 31);

    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final StadiumRepository stadiumRepository;
    private final StadiumComplexRepository stadiumComplexRepository;
    private final BookingRepository bookingRepository;
    private final MaintenanceScheduleMapper maintenanceScheduleMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean isStadiumUnderMaintenance(Stadium stadium, LocalDate date) {
        if (staticallyUnderMaintenance(stadium)) {
            return true;
        }
        StadiumComplex complex = stadium.getComplex();
        if (complex != null
                && !maintenanceScheduleRepository.findActiveForComplexAndDate(complex.getComplexId(), date).isEmpty()) {
            return true;
        }
        return !maintenanceScheduleRepository.findActiveForStadiumsAndDate(stadiumAndParentIds(stadium), date).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isComplexUnderMaintenance(StadiumComplex complex, LocalDate date) {
        if (complex.getComplexStatus() != ComplexStatus.AVAILABLE) {
            return true;
        }
        return !maintenanceScheduleRepository.findActiveForComplexAndDate(complex.getComplexId(), date).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Boolean> isUnderMaintenanceToday(List<Stadium> stadiums, LocalDate date) {
        Map<Integer, Boolean> result = new LinkedHashMap<>();
        List<Stadium> needsScheduleLookup = new ArrayList<>();
        for (Stadium stadium : stadiums) {
            if (staticallyUnderMaintenance(stadium)) {
                result.put(stadium.getStadiumId(), true);
            } else {
                needsScheduleLookup.add(stadium);
            }
        }
        if (needsScheduleLookup.isEmpty()) {
            return result;
        }

        Set<Integer> complexIds = new HashSet<>();
        Set<Integer> stadiumIds = new HashSet<>();
        for (Stadium stadium : needsScheduleLookup) {
            if (stadium.getComplex() != null) {
                complexIds.add(stadium.getComplex().getComplexId());
            }
            stadiumIds.addAll(stadiumAndParentIds(stadium));
        }

        Set<Integer> complexesUnderMaintenance = complexIds.isEmpty() ? Set.of()
                : toComplexIdSet(maintenanceScheduleRepository.findActiveForComplexesInRange(
                        List.copyOf(complexIds), date, date));
        Set<Integer> stadiumsUnderMaintenance = toStadiumIdSet(
                maintenanceScheduleRepository.findActiveForStadiumsInRange(List.copyOf(stadiumIds), date, date));

        for (Stadium stadium : needsScheduleLookup) {
            boolean underComplexSchedule = stadium.getComplex() != null
                    && complexesUnderMaintenance.contains(stadium.getComplex().getComplexId());
            boolean underStadiumSchedule = stadiumAndParentIds(stadium).stream().anyMatch(stadiumsUnderMaintenance::contains);
            result.put(stadium.getStadiumId(), underComplexSchedule || underStadiumSchedule);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<LocalDate, Boolean> isUnderMaintenanceForDateRange(Stadium stadium, LocalDate rangeStart, LocalDate rangeEnd) {
        Map<LocalDate, Boolean> result = new LinkedHashMap<>();

        if (staticallyUnderMaintenance(stadium)) {
            for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
                result.put(d, true);
            }
            return result;
        }

        StadiumComplex complex = stadium.getComplex();
        List<MaintenanceSchedule> complexSchedules = complex != null
                ? maintenanceScheduleRepository.findActiveForComplexesInRange(List.of(complex.getComplexId()), rangeStart, rangeEnd)
                : List.of();
        List<MaintenanceSchedule> stadiumSchedules = maintenanceScheduleRepository.findActiveForStadiumsInRange(
                stadiumAndParentIds(stadium), rangeStart, rangeEnd);

        for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
            LocalDate date = d;
            boolean under = complexSchedules.stream().anyMatch(s -> covers(s, date))
                    || stadiumSchedules.stream().anyMatch(s -> covers(s, date));
            result.put(date, under);
        }
        return result;
    }

    @Override
    @Transactional
    public MaintenanceScheduleResponse createSchedule(Integer stadiumId, CreateMaintenanceScheduleRequest request, Integer userId) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân với ID " + stadiumId));

        validateStadiumOwnership(stadium, userId, "đặt lịch bảo trì cho sân này");
        validateDateRange(request);

        List<Integer> affectedStadiumIds = resolveAffectedStadiumIds(stadium);
        LocalDate rangeEnd = request.getEndDate() != null ? request.getEndDate() : FAR_FUTURE;
        rejectIfConflictingBookings(affectedStadiumIds, request.getStartDate(), rangeEnd);
        rejectIfOverlappingStadiumSchedule(stadiumId, request.getStartDate(), rangeEnd);

        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .stadium(stadium)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .build();

        MaintenanceSchedule saved = maintenanceScheduleRepository.save(schedule);
        log.info("🔧 Owner {} tạo lịch bảo trì #{} cho sân {} — {} -> {}",
                userId, saved.getMaintenanceId(), stadiumId, saved.getStartDate(), saved.getEndDate());

        return toResponseWithComputedFields(saved);
    }

    @Override
    @Transactional
    public MaintenanceScheduleResponse createComplexSchedule(Integer complexId, CreateMaintenanceScheduleRequest request, Integer userId) {
        StadiumComplex complex = stadiumComplexRepository.findById(complexId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ hợp với ID " + complexId));

        validateComplexOwnership(complex, userId, "đặt lịch bảo trì cho tổ hợp này");
        validateDateRange(request);

        List<Integer> affectedCourtIds = resolveAffectedCourtIdsForComplex(complex);
        LocalDate rangeEnd = request.getEndDate() != null ? request.getEndDate() : FAR_FUTURE;
        if (!affectedCourtIds.isEmpty()) {
            rejectIfConflictingBookings(affectedCourtIds, request.getStartDate(), rangeEnd);
        }
        rejectIfOverlappingComplexSchedule(complexId, request.getStartDate(), rangeEnd);

        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .complex(complex)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .build();

        MaintenanceSchedule saved = maintenanceScheduleRepository.save(schedule);
        log.info("🔧 Owner {} tạo lịch bảo trì #{} cho tổ hợp {} — {} -> {}",
                userId, saved.getMaintenanceId(), complexId, saved.getStartDate(), saved.getEndDate());

        return toResponseWithComputedFields(saved);
    }

    @Override
    @Transactional
    public void endSchedule(Integer maintenanceId, Integer userId) {
        MaintenanceSchedule schedule = maintenanceScheduleRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch bảo trì với ID " + maintenanceId));

        if (schedule.getStadium() != null) {
            validateStadiumOwnership(schedule.getStadium(), userId, "kết thúc lịch bảo trì này");
        } else {
            validateComplexOwnership(schedule.getComplex(), userId, "kết thúc lịch bảo trì này");
        }

        LocalDate today = LocalDate.now();
        if (schedule.getEndDate() != null && schedule.getEndDate().isBefore(today)) {
            // Đã kết thúc tự nhiên từ trước (endDate < hôm nay) — "kết thúc sớm" không còn ý nghĩa gì,
            // và nếu cứ chạy tiếp logic bên dưới sẽ ĐẨY endDate về tương lai (today - 1 > endDate cũ),
            // vô tình hồi sinh 1 lịch bảo trì đã elapsed từ lâu. Coi như no-op.
            log.info("🔧 Owner {} gọi end trên lịch bảo trì #{} đã elapsed từ trước (endDate={}) — bỏ qua",
                    userId, maintenanceId, schedule.getEndDate());
            return;
        }

        // "Kết thúc ngay" nghĩa là giải phóng TỪ HÔM NAY trở đi -> endDate mục tiêu = hôm qua.
        // endDate là inclusive trong isStadiumUnderMaintenance (endDate >= date), nên set endDate = today
        // KHÔNG giải phóng được hôm nay — đây là bug đã phát hiện qua feedback thực tế, sửa lại cho đúng.
        LocalDate targetEnd = today.minusDays(1);
        if (targetEnd.isBefore(schedule.getStartDate())) {
            // Không còn ngày nào hợp lệ để giữ lại (chưa bắt đầu, hoặc bắt đầu đúng hôm nay)
            // -> endDate < startDate sẽ vi phạm CHECK constraint, nên huỷ hẳn thay vì rút ngắn.
            maintenanceScheduleRepository.delete(schedule);
            log.info("🔧 Owner {} huỷ lịch bảo trì #{} (kết thúc ngay, không còn ngày hiệu lực nào)", userId, maintenanceId);
            return;
        }

        schedule.setEndDate(targetEnd);
        maintenanceScheduleRepository.save(schedule);

        log.info("🔧 Owner {} kết thúc sớm lịch bảo trì #{} — endDate={}", userId, maintenanceId, schedule.getEndDate());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceScheduleResponse> listSchedules(Integer stadiumId, Integer userId, Pageable pageable) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân với ID " + stadiumId));
        validateStadiumOwnership(stadium, userId, "xem lịch sử bảo trì của sân này");

        return maintenanceScheduleRepository.findByStadiumStadiumIdOrderByStartDateDesc(stadiumId, pageable)
                .map(this::toResponseWithComputedFields);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceScheduleResponse> listComplexSchedules(Integer complexId, Integer userId, Pageable pageable) {
        StadiumComplex complex = stadiumComplexRepository.findById(complexId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ hợp với ID " + complexId));
        validateComplexOwnership(complex, userId, "xem lịch sử bảo trì của tổ hợp này");

        return maintenanceScheduleRepository.findByComplexComplexIdOrderByStartDateDesc(complexId, pageable)
                .map(this::toResponseWithComputedFields);
    }

    private boolean staticallyUnderMaintenance(Stadium stadium) {
        if (stadium.getStadiumStatus() != StadiumStatus.AVAILABLE) {
            return true;
        }
        Stadium parent = stadium.getParentStadium();
        if (parent != null && parent.getStadiumStatus() != StadiumStatus.AVAILABLE) {
            return true;
        }
        StadiumComplex complex = stadium.getComplex();
        return complex != null && complex.getComplexStatus() != ComplexStatus.AVAILABLE;
    }

    private List<Integer> stadiumAndParentIds(Stadium stadium) {
        Stadium parent = stadium.getParentStadium();
        return parent != null
                ? List.of(stadium.getStadiumId(), parent.getStadiumId())
                : List.of(stadium.getStadiumId());
    }

    private boolean covers(MaintenanceSchedule schedule, LocalDate date) {
        return !schedule.getStartDate().isAfter(date)
                && (schedule.getEndDate() == null || !schedule.getEndDate().isBefore(date));
    }

    private Set<Integer> toComplexIdSet(List<MaintenanceSchedule> schedules) {
        Set<Integer> ids = new HashSet<>();
        for (MaintenanceSchedule s : schedules) {
            if (s.getComplex() != null) {
                ids.add(s.getComplex().getComplexId());
            }
        }
        return ids;
    }

    private Set<Integer> toStadiumIdSet(List<MaintenanceSchedule> schedules) {
        Set<Integer> ids = new HashSet<>();
        for (MaintenanceSchedule s : schedules) {
            if (s.getStadium() != null) {
                ids.add(s.getStadium().getStadiumId());
            }
        }
        return ids;
    }

    private void validateDateRange(CreateMaintenanceScheduleRequest request) {
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Ngày bắt đầu không được ở trong quá khứ");
        }
    }

    private void rejectIfConflictingBookings(List<Integer> affectedStadiumIds, LocalDate startDate, LocalDate rangeEnd) {
        List<Booking> conflicts = bookingRepository.findByStadiumIdsAndDateRangeAndStatuses(
                affectedStadiumIds, startDate, rangeEnd, CONFLICT_STATUSES);
        if (!conflicts.isEmpty()) {
            throw new BadRequestException(
                    "Không thể tạo lịch bảo trì: có " + conflicts.size()
                            + " booking đã xác nhận (CONFIRMED) rơi vào khung này. "
                            + "Vui lòng tự xử lý các booking đó trước khi đặt bảo trì.");
        }
    }

    /** Chặn tạo lịch bảo trì mới nếu đã có 1 lịch khác (chưa kết thúc) trùng khung ngày cho ĐÚNG sân này. */
    private void rejectIfOverlappingStadiumSchedule(Integer stadiumId, LocalDate startDate, LocalDate rangeEnd) {
        List<MaintenanceSchedule> overlapping = maintenanceScheduleRepository.findActiveForStadiumsInRange(
                List.of(stadiumId), startDate, rangeEnd);
        if (!overlapping.isEmpty()) {
            throw new BadRequestException(
                    "Sân này đã có lịch bảo trì khác trùng khung ngày — vui lòng kết thúc lịch cũ trước khi tạo mới.");
        }
    }

    /** Tương tự {@link #rejectIfOverlappingStadiumSchedule} nhưng cho lịch bảo trì gắn trực tiếp ở cấp Complex. */
    private void rejectIfOverlappingComplexSchedule(Integer complexId, LocalDate startDate, LocalDate rangeEnd) {
        List<MaintenanceSchedule> overlapping = maintenanceScheduleRepository.findActiveForComplexesInRange(
                List.of(complexId), startDate, rangeEnd);
        if (!overlapping.isEmpty()) {
            throw new BadRequestException(
                    "Tổ hợp này đã có lịch bảo trì khác trùng khung ngày — vui lòng kết thúc lịch cũ trước khi tạo mới.");
        }
    }

    private void validateStadiumOwnership(Stadium stadium, Integer userId, String action) {
        Owner resolvedOwner = stadium.resolveOwner();
        if (resolvedOwner == null || resolvedOwner.getUser() == null
                || !resolvedOwner.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền " + action);
        }
    }

    private void validateComplexOwnership(StadiumComplex complex, Integer userId, String action) {
        Owner owner = complex.getOwner();
        if (owner == null || owner.getUser() == null || !owner.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền " + action);
        }
    }

    /** FACILITY -> chính nó + toàn bộ court con (cascade). COURT -> chỉ chính nó. */
    private List<Integer> resolveAffectedStadiumIds(Stadium stadium) {
        if (stadium.getNodeType() == StadiumNodeType.FACILITY) {
            List<Integer> ids = new ArrayList<>();
            ids.add(stadium.getStadiumId());
            stadium.getChildCourts().forEach(court -> ids.add(court.getStadiumId()));
            return ids;
        }
        return List.of(stadium.getStadiumId());
    }

    /**
     * Toàn bộ COURT (đơn vị nhận booking) thuộc mọi FACILITY trong Complex — dùng để check
     * conflict khi bảo trì đặt ở cấp Complex (cascade 2 bậc: Complex -> Facility -> Court).
     */
    private List<Integer> resolveAffectedCourtIdsForComplex(StadiumComplex complex) {
        return complex.getStadiums().stream()
                .filter(s -> s.getNodeType() == StadiumNodeType.COURT)
                .map(Stadium::getStadiumId)
                .toList();
    }

    private MaintenanceScheduleResponse toResponseWithComputedFields(MaintenanceSchedule schedule) {
        MaintenanceScheduleResponse response = maintenanceScheduleMapper.toResponse(schedule);
        LocalDate today = LocalDate.now();
        boolean indefinite = schedule.getEndDate() == null;
        boolean active = !schedule.getStartDate().isAfter(today)
                && (indefinite || !schedule.getEndDate().isBefore(today));
        response.setIndefinite(indefinite);
        response.setActive(active);
        return response;
    }
}
