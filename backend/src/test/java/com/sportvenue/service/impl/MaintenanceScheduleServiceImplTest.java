package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateMaintenanceScheduleRequest;
import com.sportvenue.dto.response.MaintenanceScheduleResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.MaintenanceSchedule;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link MaintenanceScheduleServiceImpl} — single source of truth
 * {@code isStadiumUnderMaintenance}, conflict-check khi tạo lịch, và bug đã fix ở
 * {@code endSchedule} (endDate inclusive khiến "kết thúc hôm nay" không giải phóng
 * được hôm nay nếu chỉ set endDate = today).
 */
@ExtendWith(MockitoExtension.class)
class MaintenanceScheduleServiceImplTest {

    private static final Integer OWNER_USER_ID = 1;
    private static final Integer OTHER_USER_ID = 2;

    @Mock private MaintenanceScheduleRepository maintenanceScheduleRepository;
    @Mock private StadiumRepository stadiumRepository;
    @Mock private StadiumComplexRepository stadiumComplexRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private MaintenanceScheduleMapper maintenanceScheduleMapper;

    private MaintenanceScheduleServiceImpl service;
    private Owner owner;

    @BeforeEach
    void setUp() {
        service = new MaintenanceScheduleServiceImpl(
                maintenanceScheduleRepository, stadiumRepository, stadiumComplexRepository,
                bookingRepository, maintenanceScheduleMapper);

        User ownerUser = User.builder().userId(OWNER_USER_ID).email("owner@sportvenue.com").build();
        owner = Owner.builder().ownerId(1).user(ownerUser).build();

        // Mirror hành vi thật của mapper — copy field cơ bản, indefinite/active service tự tính sau.
        // lenient(): nhiều test (VD: not-found, not-owner, isStadiumUnderMaintenance) không bao giờ chạm
        // tới mapper vì exception ném ra trước đó — không phải lỗi test, không cần stub riêng từng case.
        lenient().when(maintenanceScheduleMapper.toResponse(any(MaintenanceSchedule.class))).thenAnswer(invocation -> {
            MaintenanceSchedule s = invocation.getArgument(0);
            return MaintenanceScheduleResponse.builder()
                    .maintenanceId(s.getMaintenanceId())
                    .stadiumId(s.getStadium() != null ? s.getStadium().getStadiumId() : null)
                    .complexId(s.getComplex() != null ? s.getComplex().getComplexId() : null)
                    .startDate(s.getStartDate())
                    .endDate(s.getEndDate())
                    .reason(s.getReason())
                    .createdAt(s.getCreatedAt())
                    .build();
        });
    }

    // ── fixtures — Complex -> Facility -> Court luôn nối đúng, vì Stadium.resolveOwner()
    // với COURT bắt buộc đi qua parentStadium.getComplex().getOwner(). ──────────────────

    private StadiumComplex newComplex(ComplexStatus status) {
        return StadiumComplex.builder()
                .complexId(1)
                .owner(owner)
                .complexStatus(status)
                .stadiums(new LinkedHashSet<>())
                .build();
    }

    private Stadium newFacility(Integer id, StadiumComplex complex, StadiumStatus status) {
        return Stadium.builder()
                .stadiumId(id)
                .stadiumName("Facility " + id)
                .nodeType(StadiumNodeType.FACILITY)
                .stadiumStatus(status)
                .complex(complex)
                .childCourts(new LinkedHashSet<>())
                .build();
    }

    private Stadium newCourt(Integer id, Stadium facility, StadiumStatus status) {
        return Stadium.builder()
                .stadiumId(id)
                .stadiumName("Court " + id)
                .nodeType(StadiumNodeType.COURT)
                .stadiumStatus(status)
                .parentStadium(facility)
                .complex(facility.getComplex())
                .build();
    }

    /** Booking active giả lập với slot 08:00-09:00 ngày {@code date} — dùng cho conflict-check. */
    private Booking conflictingBooking(LocalDate date) {
        TimeSlot slot = TimeSlot.builder().slotId(1).startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0)).build();
        return Booking.builder().reservationDate(date).slot(slot).build();
    }

    private CreateMaintenanceScheduleRequest validRequest() {
        return CreateMaintenanceScheduleRequest.builder()
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .reason("Test")
                .build();
    }

    // ── isStadiumUnderMaintenance ──────────────────────────────────────────

    @Test
    void isStadiumUnderMaintenance_stadiumStatusNotAvailable_returnsTrue() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.MAINTENANCE);

        assertTrue(service.isStadiumUnderMaintenance(court, LocalDate.now()));
        verifyNoInteractions(maintenanceScheduleRepository);
    }

    @Test
    void isStadiumUnderMaintenance_parentFacilitySuspended_returnsTrue() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.MAINTENANCE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);

        assertTrue(service.isStadiumUnderMaintenance(court, LocalDate.now()));
    }

    @Test
    void isStadiumUnderMaintenance_complexSuspended_returnsTrue() {
        StadiumComplex complex = newComplex(ComplexStatus.MAINTENANCE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);

        assertTrue(service.isStadiumUnderMaintenance(court, LocalDate.now()));
    }

    @Test
    void isStadiumUnderMaintenance_activeComplexLevelSchedule_returnsTrue() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        LocalDate date = LocalDate.now();

        when(maintenanceScheduleRepository.findActiveForComplexAndDate(complex.getComplexId(), date))
                .thenReturn(List.of(MaintenanceSchedule.builder().complex(complex).build()));

        assertTrue(service.isStadiumUnderMaintenance(court, date));
    }

    @Test
    void isStadiumUnderMaintenance_activeStadiumLevelSchedule_returnsTrue() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        LocalDate date = LocalDate.now();

        when(maintenanceScheduleRepository.findActiveForComplexAndDate(complex.getComplexId(), date))
                .thenReturn(List.of());
        when(maintenanceScheduleRepository.findActiveForStadiumsAndDate(
                List.of(court.getStadiumId(), facility.getStadiumId()), date))
                .thenReturn(List.of(MaintenanceSchedule.builder().stadium(court).build()));

        assertTrue(service.isStadiumUnderMaintenance(court, date));
    }

    @Test
    void isStadiumUnderMaintenance_everythingClean_returnsFalse() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        LocalDate date = LocalDate.now();

        when(maintenanceScheduleRepository.findActiveForComplexAndDate(complex.getComplexId(), date))
                .thenReturn(List.of());
        when(maintenanceScheduleRepository.findActiveForStadiumsAndDate(anyList(), eq(date)))
                .thenReturn(List.of());

        assertFalse(service.isStadiumUnderMaintenance(court, date));
    }

    // ── isStadiumUnderMaintenanceNow / isUnderMaintenanceNow ────────────────
    // Dùng khoảng thời gian rộng (±1 ngày hoặc ±1 năm) quanh "now" thay vì cộng/trừ giờ cụ thể,
    // để test không flaky bất kể chạy vào giờ nào trong ngày (tránh vấn đề lệch nửa đêm).

    @Test
    void isStadiumUnderMaintenanceNow_stadiumStatusNotAvailable_returnsTrue() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.MAINTENANCE);

        assertTrue(service.isStadiumUnderMaintenanceNow(court));
        verifyNoInteractions(maintenanceScheduleRepository);
    }

    @Test
    void isStadiumUnderMaintenanceNow_scheduleCoversCurrentInstant_returnsTrue() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        LocalDate today = LocalDate.now();

        MaintenanceSchedule activeNow = MaintenanceSchedule.builder()
                .stadium(court)
                .startDate(today.minusDays(1)).startTime(LocalTime.MIN)
                .endDate(today.plusDays(1)).endTime(LocalTime.MAX)
                .build();

        when(maintenanceScheduleRepository.findActiveForComplexAndDate(eq(complex.getComplexId()), eq(today)))
                .thenReturn(List.of());
        when(maintenanceScheduleRepository.findActiveForStadiumsAndDate(anyList(), eq(today)))
                .thenReturn(List.of(activeNow));

        assertTrue(service.isStadiumUnderMaintenanceNow(court));
    }

    @Test
    void isStadiumUnderMaintenanceNow_scheduleNotYetStarted_returnsFalse() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        LocalDate today = LocalDate.now();

        // Chưa bắt đầu — hiệu lực từ 1 năm sau trở đi (dù giả lập là "ứng viên của hôm nay").
        LocalDate farFuture = today.plusYears(1);
        MaintenanceSchedule upcoming = MaintenanceSchedule.builder()
                .stadium(court)
                .startDate(farFuture).endDate(farFuture.plusDays(1))
                .build();

        when(maintenanceScheduleRepository.findActiveForComplexAndDate(eq(complex.getComplexId()), eq(today)))
                .thenReturn(List.of());
        when(maintenanceScheduleRepository.findActiveForStadiumsAndDate(anyList(), eq(today)))
                .thenReturn(List.of(upcoming));

        assertFalse(service.isStadiumUnderMaintenanceNow(court));
    }

    @Test
    void isStadiumUnderMaintenanceNow_scheduleAlreadyEnded_returnsFalse() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        LocalDate today = LocalDate.now();

        LocalDate longAgo = today.minusYears(1);
        MaintenanceSchedule ended = MaintenanceSchedule.builder()
                .stadium(court)
                .startDate(longAgo).endDate(longAgo.plusDays(1))
                .build();

        when(maintenanceScheduleRepository.findActiveForComplexAndDate(eq(complex.getComplexId()), eq(today)))
                .thenReturn(List.of());
        when(maintenanceScheduleRepository.findActiveForStadiumsAndDate(anyList(), eq(today)))
                .thenReturn(List.of(ended));

        assertFalse(service.isStadiumUnderMaintenanceNow(court));
    }

    @Test
    void isUnderMaintenanceNow_batch_mixesActiveUpcomingAndClean() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium activeCourt = newCourt(20, facility, StadiumStatus.AVAILABLE);
        Stadium upcomingCourt = newCourt(21, facility, StadiumStatus.AVAILABLE);
        Stadium cleanCourt = newCourt(22, facility, StadiumStatus.AVAILABLE);
        LocalDate today = LocalDate.now();

        MaintenanceSchedule activeNow = MaintenanceSchedule.builder()
                .stadium(activeCourt)
                .startDate(today.minusDays(1)).startTime(LocalTime.MIN)
                .endDate(today.plusDays(1)).endTime(LocalTime.MAX)
                .build();
        MaintenanceSchedule upcoming = MaintenanceSchedule.builder()
                .stadium(upcomingCourt)
                .startDate(today.plusYears(1)).endDate(today.plusYears(1).plusDays(1))
                .build();

        when(maintenanceScheduleRepository.findActiveForComplexesInRange(anyList(), eq(today), eq(today)))
                .thenReturn(List.of());
        when(maintenanceScheduleRepository.findActiveForStadiumsInRange(anyList(), eq(today), eq(today)))
                .thenReturn(List.of(activeNow, upcoming));

        Map<Integer, Boolean> result = service.isUnderMaintenanceNow(List.of(activeCourt, upcomingCourt, cleanCourt));

        assertTrue(result.get(activeCourt.getStadiumId()));
        assertFalse(result.get(upcomingCourt.getStadiumId()));
        assertFalse(result.get(cleanCourt.getStadiumId()));
    }

    // ── createSchedule (Stadium level) ─────────────────────────────────────

    @Test
    void createSchedule_stadiumNotFound_throwsResourceNotFound() {
        when(stadiumRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createSchedule(99, validRequest(), OWNER_USER_ID));
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_notOwner_throwsAccessDenied() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));

        assertThrows(AccessDeniedException.class,
                () -> service.createSchedule(court.getStadiumId(), validRequest(), OTHER_USER_ID));
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_endDateBeforeStartDate_throwsBadRequest() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));

        CreateMaintenanceScheduleRequest request = CreateMaintenanceScheduleRequest.builder()
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(1))
                .build();

        assertThrows(BadRequestException.class,
                () -> service.createSchedule(court.getStadiumId(), request, OWNER_USER_ID));
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_pastStartDate_throwsBadRequest() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));

        CreateMaintenanceScheduleRequest request = CreateMaintenanceScheduleRequest.builder()
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();

        assertThrows(BadRequestException.class,
                () -> service.createSchedule(court.getStadiumId(), request, OWNER_USER_ID));
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    /** Regression test — cùng 1 sân không được có 2 lịch bảo trì chồng khung ngày. */
    @Test
    void createSchedule_overlapsExistingStadiumSchedule_throwsBadRequest() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));
        when(bookingRepository.findByStadiumIdsAndDateRangeAndStatuses(anyList(), any(), any(), anyList()))
                .thenReturn(List.of());

        CreateMaintenanceScheduleRequest request = validRequest();
        when(maintenanceScheduleRepository.findActiveForStadiumsInRange(
                eq(List.of(court.getStadiumId())), eq(request.getStartDate()), any()))
                .thenReturn(List.of(MaintenanceSchedule.builder().maintenanceId(999).stadium(court)
                        .startDate(request.getStartDate()).endDate(request.getEndDate())
                        .build()));

        assertThrows(BadRequestException.class,
                () -> service.createSchedule(court.getStadiumId(), request, OWNER_USER_ID));
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_facilityCascadesToChildCourts_conflictBlocksCreation() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium child1 = newCourt(101, facility, StadiumStatus.AVAILABLE);
        Stadium child2 = newCourt(102, facility, StadiumStatus.AVAILABLE);
        facility.setChildCourts(new LinkedHashSet<>(Set.of(child1, child2)));
        when(stadiumRepository.findById(facility.getStadiumId())).thenReturn(Optional.of(facility));

        CreateMaintenanceScheduleRequest request = validRequest();
        when(bookingRepository.findByStadiumIdsAndDateRangeAndStatuses(
                anyList(), eq(request.getStartDate()), any(), anyList()))
                .thenReturn(List.of(conflictingBooking(request.getStartDate())));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createSchedule(facility.getStadiumId(), request, OWNER_USER_ID));
        assertTrue(ex.getMessage().contains("1 booking"));

        ArgumentCaptor<List<Integer>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository).findByStadiumIdsAndDateRangeAndStatuses(
                idsCaptor.capture(), any(), any(), anyList());
        assertTrue(idsCaptor.getValue().containsAll(
                List.of(facility.getStadiumId(), child1.getStadiumId(), child2.getStadiumId())));
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_court_onlyChecksItself_andSavesIndefiniteSchedule() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));
        when(bookingRepository.findByStadiumIdsAndDateRangeAndStatuses(anyList(), any(), any(), anyList()))
                .thenReturn(List.of());
        when(maintenanceScheduleRepository.save(any(MaintenanceSchedule.class))).thenAnswer(invocation -> {
            MaintenanceSchedule s = invocation.getArgument(0);
            s.setMaintenanceId(500);
            return s;
        });

        CreateMaintenanceScheduleRequest request = CreateMaintenanceScheduleRequest.builder()
                .startDate(LocalDate.now())
                .reason("Sua mat san")
                .build();

        MaintenanceScheduleResponse response = service.createSchedule(court.getStadiumId(), request, OWNER_USER_ID);

        assertEquals(500, response.getMaintenanceId());
        assertEquals(court.getStadiumId(), response.getStadiumId());
        assertTrue(response.getIndefinite());
        assertTrue(response.getActive());

        verify(bookingRepository).findByStadiumIdsAndDateRangeAndStatuses(
                eq(List.of(court.getStadiumId())), eq(request.getStartDate()), any(), anyList());
    }

    // ── createComplexSchedule ───────────────────────────────────────────────

    @Test
    void createComplexSchedule_complexNotFound_throwsResourceNotFound() {
        when(stadiumComplexRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createComplexSchedule(99, validRequest(), OWNER_USER_ID));
    }

    @Test
    void createComplexSchedule_notOwner_throwsAccessDenied() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        when(stadiumComplexRepository.findById(complex.getComplexId())).thenReturn(Optional.of(complex));

        assertThrows(AccessDeniedException.class,
                () -> service.createComplexSchedule(complex.getComplexId(), validRequest(), OTHER_USER_ID));
    }

    @Test
    void createComplexSchedule_conflictOnCourtUnderFacility_throwsBadRequest() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(201, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(101, facility, StadiumStatus.AVAILABLE);
        complex.setStadiums(new LinkedHashSet<>(Set.of(facility, court)));
        when(stadiumComplexRepository.findById(complex.getComplexId())).thenReturn(Optional.of(complex));

        CreateMaintenanceScheduleRequest request = validRequest();
        when(bookingRepository.findByStadiumIdsAndDateRangeAndStatuses(
                eq(List.of(court.getStadiumId())), any(), any(), anyList()))
                .thenReturn(List.of(conflictingBooking(request.getStartDate())));

        assertThrows(BadRequestException.class,
                () -> service.createComplexSchedule(complex.getComplexId(), request, OWNER_USER_ID));
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    @Test
    void createComplexSchedule_noCourtsUnderComplex_skipsConflictCheckAndSaves() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        when(stadiumComplexRepository.findById(complex.getComplexId())).thenReturn(Optional.of(complex));
        when(maintenanceScheduleRepository.save(any(MaintenanceSchedule.class))).thenAnswer(invocation -> {
            MaintenanceSchedule s = invocation.getArgument(0);
            s.setMaintenanceId(600);
            return s;
        });

        MaintenanceScheduleResponse response =
                service.createComplexSchedule(complex.getComplexId(), validRequest(), OWNER_USER_ID);

        assertEquals(complex.getComplexId(), response.getComplexId());
        verify(bookingRepository, never()).findByStadiumIdsAndDateRangeAndStatuses(any(), any(), any(), any());
    }

    // ── endSchedule ──────────────────────────────────────────────────────────

    @Test
    void endSchedule_notFound_throwsResourceNotFound() {
        when(maintenanceScheduleRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.endSchedule(1, OWNER_USER_ID));
    }

    @Test
    void endSchedule_notOwner_stadiumScoped_throwsAccessDenied() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .maintenanceId(1).stadium(court).startDate(LocalDate.now().minusDays(5)).build();
        when(maintenanceScheduleRepository.findById(1)).thenReturn(Optional.of(schedule));

        assertThrows(AccessDeniedException.class, () -> service.endSchedule(1, OTHER_USER_ID));
        verify(maintenanceScheduleRepository, never()).save(any());
        verify(maintenanceScheduleRepository, never()).delete(any());
    }

    @Test
    void endSchedule_notOwner_complexScoped_throwsAccessDenied() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .maintenanceId(1).complex(complex).startDate(LocalDate.now().minusDays(5)).build();
        when(maintenanceScheduleRepository.findById(1)).thenReturn(Optional.of(schedule));

        assertThrows(AccessDeniedException.class, () -> service.endSchedule(1, OTHER_USER_ID));
    }

    /**
     * Regression test — bug thật phát hiện qua review: gọi end trên 1 lịch ĐÃ elapsed từ lâu
     * (endDate < hôm nay) trước đây sẽ tính targetEnd = today-1, có thể MUỘN HƠN endDate cũ,
     * vô tình đẩy endDate về tương lai và "hồi sinh" lịch bảo trì đã kết thúc từ lâu. Giờ phải
     * là no-op hoàn toàn.
     */
    @Test
    void endSchedule_alreadyElapsed_isNoOp() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        LocalDate originalEndDate = LocalDate.now().minusDays(30);
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .maintenanceId(1)
                .stadium(court)
                .startDate(LocalDate.now().minusDays(35))
                .endDate(originalEndDate)
                .build();
        when(maintenanceScheduleRepository.findById(1)).thenReturn(Optional.of(schedule));

        service.endSchedule(1, OWNER_USER_ID);

        assertEquals(originalEndDate, schedule.getEndDate(), "endDate không được đổi khi lịch đã elapsed từ trước");
        verify(maintenanceScheduleRepository, never()).save(any());
        verify(maintenanceScheduleRepository, never()).delete(any());
    }

    @Test
    void endSchedule_startedBeforeToday_shrinksEndDateToYesterday() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .maintenanceId(1)
                .stadium(court)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(5))
                .build();
        when(maintenanceScheduleRepository.findById(1)).thenReturn(Optional.of(schedule));

        service.endSchedule(1, OWNER_USER_ID);

        assertEquals(LocalDate.now().minusDays(1), schedule.getEndDate());
        verify(maintenanceScheduleRepository).save(schedule);
        verify(maintenanceScheduleRepository, never()).delete(any());
    }

    /** Regression test — bug thực tế đã phát hiện: set endDate=today không giải phóng được hôm nay. */
    @Test
    void endSchedule_startedToday_deletesInsteadOfShrinking() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .maintenanceId(1)
                .stadium(court)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .build();
        when(maintenanceScheduleRepository.findById(1)).thenReturn(Optional.of(schedule));

        service.endSchedule(1, OWNER_USER_ID);

        verify(maintenanceScheduleRepository).delete(schedule);
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    @Test
    void endSchedule_startsInFuture_deletesEntirely() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .maintenanceId(1)
                .stadium(court)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(5))
                .build();
        when(maintenanceScheduleRepository.findById(1)).thenReturn(Optional.of(schedule));

        service.endSchedule(1, OWNER_USER_ID);

        verify(maintenanceScheduleRepository).delete(schedule);
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    // ── listSchedules / listComplexSchedules ───────────────────────────────

    @Test
    void listSchedules_notOwner_throwsAccessDenied() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));

        assertThrows(AccessDeniedException.class,
                () -> service.listSchedules(court.getStadiumId(), OTHER_USER_ID, Pageable.unpaged()));
    }

    @Test
    void listSchedules_mapsIndefiniteAndActiveCorrectly() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));

        MaintenanceSchedule indefiniteActive = MaintenanceSchedule.builder()
                .maintenanceId(1).stadium(court)
                .startDate(LocalDate.now().minusDays(1)).endDate(null)
                .build();
        MaintenanceSchedule datedFuture = MaintenanceSchedule.builder()
                .maintenanceId(2).stadium(court)
                .startDate(LocalDate.now().plusDays(10)).endDate(LocalDate.now().plusDays(12))
                .build();
        Pageable pageable = PageRequest.of(0, 20);
        when(maintenanceScheduleRepository.findByStadiumStadiumIdOrderByStartDateDesc(court.getStadiumId(), pageable))
                .thenReturn(new PageImpl<>(List.of(indefiniteActive, datedFuture)));

        Page<MaintenanceScheduleResponse> result = service.listSchedules(court.getStadiumId(), OWNER_USER_ID, pageable);

        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().get(0).getIndefinite());
        assertTrue(result.getContent().get(0).getActive());
        assertFalse(result.getContent().get(1).getIndefinite());
        assertFalse(result.getContent().get(1).getActive());
    }

    @Test
    void listComplexSchedules_notOwner_throwsAccessDenied() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        when(stadiumComplexRepository.findById(complex.getComplexId())).thenReturn(Optional.of(complex));

        assertThrows(AccessDeniedException.class,
                () -> service.listComplexSchedules(complex.getComplexId(), OTHER_USER_ID, Pageable.unpaged()));
    }

    @Test
    void listComplexSchedules_delegatesToComplexRepositoryQuery() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        when(stadiumComplexRepository.findById(complex.getComplexId())).thenReturn(Optional.of(complex));
        Pageable pageable = PageRequest.of(0, 20);
        when(maintenanceScheduleRepository.findByComplexComplexIdOrderByStartDateDesc(complex.getComplexId(), pageable))
                .thenReturn(Page.empty());

        Page<MaintenanceScheduleResponse> result =
                service.listComplexSchedules(complex.getComplexId(), OWNER_USER_ID, pageable);

        assertTrue(result.isEmpty());
        verify(maintenanceScheduleRepository, times(1))
                .findByComplexComplexIdOrderByStartDateDesc(complex.getComplexId(), pageable);
    }

    // ── Mở rộng theo khung giờ (startTime/endTime) ─────────────────────────

    @Test
    void createSchedule_endTimeWithoutEndDate_throwsBadRequest() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));

        CreateMaintenanceScheduleRequest request = CreateMaintenanceScheduleRequest.builder()
                .startDate(LocalDate.now())
                .endTime(LocalTime.of(10, 0))
                .build();

        assertThrows(BadRequestException.class,
                () -> service.createSchedule(court.getStadiumId(), request, OWNER_USER_ID));
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_endTimeBeforeStartTimeSameDay_throwsBadRequest() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));

        LocalDate today = LocalDate.now();
        CreateMaintenanceScheduleRequest request = CreateMaintenanceScheduleRequest.builder()
                .startDate(today).endDate(today)
                .startTime(LocalTime.of(16, 0)).endTime(LocalTime.of(14, 0))
                .build();

        assertThrows(BadRequestException.class,
                () -> service.createSchedule(court.getStadiumId(), request, OWNER_USER_ID));
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_nonOverlappingTimeWindowsSameDay_savesSuccessfully() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));
        when(bookingRepository.findByStadiumIdsAndDateRangeAndStatuses(anyList(), any(), any(), anyList()))
                .thenReturn(List.of());
        when(maintenanceScheduleRepository.save(any(MaintenanceSchedule.class))).thenAnswer(invocation -> {
            MaintenanceSchedule s = invocation.getArgument(0);
            s.setMaintenanceId(700);
            return s;
        });

        LocalDate day = LocalDate.now().plusDays(1);
        // Lịch đã tồn tại: 20h-22h ngày mai — không chồng với lịch mới 14h-16h.
        when(maintenanceScheduleRepository.findActiveForStadiumsInRange(List.of(court.getStadiumId()), day, day))
                .thenReturn(List.of(MaintenanceSchedule.builder().maintenanceId(1).stadium(court)
                        .startDate(day).endDate(day)
                        .startTime(LocalTime.of(20, 0)).endTime(LocalTime.of(22, 0))
                        .build()));

        CreateMaintenanceScheduleRequest request = CreateMaintenanceScheduleRequest.builder()
                .startDate(day).endDate(day)
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .build();

        MaintenanceScheduleResponse response = service.createSchedule(court.getStadiumId(), request, OWNER_USER_ID);

        assertEquals(700, response.getMaintenanceId());
        verify(maintenanceScheduleRepository).save(any(MaintenanceSchedule.class));
    }

    @Test
    void createSchedule_overlappingTimeWindowsSameDay_throwsBadRequest() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));
        when(bookingRepository.findByStadiumIdsAndDateRangeAndStatuses(anyList(), any(), any(), anyList()))
                .thenReturn(List.of());

        LocalDate day = LocalDate.now().plusDays(1);
        // Lịch đã tồn tại: 14h-16h ngày mai — chồng với lịch mới 15h-17h.
        when(maintenanceScheduleRepository.findActiveForStadiumsInRange(List.of(court.getStadiumId()), day, day))
                .thenReturn(List.of(MaintenanceSchedule.builder().maintenanceId(1).stadium(court)
                        .startDate(day).endDate(day)
                        .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                        .build()));

        CreateMaintenanceScheduleRequest request = CreateMaintenanceScheduleRequest.builder()
                .startDate(day).endDate(day)
                .startTime(LocalTime.of(15, 0)).endTime(LocalTime.of(17, 0))
                .build();

        assertThrows(BadRequestException.class,
                () -> service.createSchedule(court.getStadiumId(), request, OWNER_USER_ID));
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_bookingOutsideTimeWindow_doesNotConflict() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));
        when(maintenanceScheduleRepository.findActiveForStadiumsInRange(anyList(), any(), any())).thenReturn(List.of());
        when(maintenanceScheduleRepository.save(any(MaintenanceSchedule.class))).thenAnswer(invocation -> {
            MaintenanceSchedule s = invocation.getArgument(0);
            s.setMaintenanceId(701);
            return s;
        });

        LocalDate day = LocalDate.now().plusDays(1);
        // Booking 08:00-09:00 ngày mai — bảo trì chỉ chặn 14h-16h -> không đụng nhau.
        when(bookingRepository.findByStadiumIdsAndDateRangeAndStatuses(anyList(), eq(day), eq(day), anyList()))
                .thenReturn(List.of(conflictingBooking(day)));

        CreateMaintenanceScheduleRequest request = CreateMaintenanceScheduleRequest.builder()
                .startDate(day).endDate(day)
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .build();

        MaintenanceScheduleResponse response = service.createSchedule(court.getStadiumId(), request, OWNER_USER_ID);
        assertEquals(701, response.getMaintenanceId());
    }

    @Test
    void createSchedule_bookingInsideTimeWindow_throwsBadRequest() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));

        LocalDate day = LocalDate.now().plusDays(1);
        // Booking 08:00-09:00 ngày mai — bảo trì chặn 07:30-08:30 -> chồng lấn 08:00-08:30.
        // rejectIfConflictingBookings ném exception trước khi tới rejectIfOverlappingStadiumSchedule,
        // nên không cần stub findActiveForStadiumsInRange (không bao giờ được gọi tới ở nhánh này).
        when(bookingRepository.findByStadiumIdsAndDateRangeAndStatuses(anyList(), eq(day), eq(day), anyList()))
                .thenReturn(List.of(conflictingBooking(day)));

        CreateMaintenanceScheduleRequest request = CreateMaintenanceScheduleRequest.builder()
                .startDate(day).endDate(day)
                .startTime(LocalTime.of(7, 30)).endTime(LocalTime.of(8, 30))
                .build();

        assertThrows(BadRequestException.class,
                () -> service.createSchedule(court.getStadiumId(), request, OWNER_USER_ID));
        verify(maintenanceScheduleRepository, never()).save(any());
    }

    @Test
    void isSlotUnderMaintenance_slotOutsideScheduleWindow_returnsFalse() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        LocalDate date = LocalDate.now();

        MaintenanceSchedule schedule = MaintenanceSchedule.builder().stadium(court)
                .startDate(date).endDate(date)
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .build();
        when(maintenanceScheduleRepository.findActiveForComplexAndDate(complex.getComplexId(), date))
                .thenReturn(List.of());
        when(maintenanceScheduleRepository.findActiveForStadiumsAndDate(anyList(), eq(date)))
                .thenReturn(List.of(schedule));

        assertFalse(service.isSlotUnderMaintenance(court, date, LocalTime.of(9, 0), LocalTime.of(10, 0)));
    }

    @Test
    void isSlotUnderMaintenance_slotOverlapsScheduleWindow_returnsTrue() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        LocalDate date = LocalDate.now();

        MaintenanceSchedule schedule = MaintenanceSchedule.builder().stadium(court)
                .startDate(date).endDate(date)
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .build();
        when(maintenanceScheduleRepository.findActiveForComplexAndDate(complex.getComplexId(), date))
                .thenReturn(List.of());
        when(maintenanceScheduleRepository.findActiveForStadiumsAndDate(anyList(), eq(date)))
                .thenReturn(List.of(schedule));

        assertTrue(service.isSlotUnderMaintenance(court, date, LocalTime.of(15, 0), LocalTime.of(16, 30)));
    }

    @Test
    void getDayMaintenanceForDateRange_hourScopedSchedule_onlyOverlappingSlotFlagged() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        MaintenanceSchedule schedule = MaintenanceSchedule.builder().stadium(court)
                .startDate(today).endDate(today)
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .build();
        when(maintenanceScheduleRepository.findActiveForComplexesInRange(anyList(), eq(today), eq(tomorrow)))
                .thenReturn(List.of());
        when(maintenanceScheduleRepository.findActiveForStadiumsInRange(anyList(), eq(today), eq(tomorrow)))
                .thenReturn(List.of(schedule));

        var result = service.getDayMaintenanceForDateRange(court, today, tomorrow);

        assertFalse(result.get(today).overlaps(LocalTime.of(9, 0), LocalTime.of(10, 0), today));
        assertTrue(result.get(today).overlaps(LocalTime.of(15, 0), LocalTime.of(16, 30), today));
        assertFalse(result.get(tomorrow).overlaps(LocalTime.of(15, 0), LocalTime.of(16, 30), tomorrow));
    }

    @Test
    void listSchedules_hourScopedSchedule_activeReflectsCurrentInstant() {
        StadiumComplex complex = newComplex(ComplexStatus.AVAILABLE);
        Stadium facility = newFacility(10, complex, StadiumStatus.AVAILABLE);
        Stadium court = newCourt(20, facility, StadiumStatus.AVAILABLE);
        when(stadiumRepository.findById(court.getStadiumId())).thenReturn(Optional.of(court));

        LocalDate today = LocalDate.now();
        // Khung giờ bao trùm cả ngày hôm nay [00:00, 23:59:59] -> chắc chắn active bất kể giờ chạy test.
        MaintenanceSchedule allDayToday = MaintenanceSchedule.builder()
                .maintenanceId(1).stadium(court)
                .startDate(today).endDate(today)
                .startTime(LocalTime.MIN).endTime(LocalTime.MAX)
                .build();
        // Đã kết thúc hẳn từ hôm qua -> chắc chắn không active.
        MaintenanceSchedule endedYesterday = MaintenanceSchedule.builder()
                .maintenanceId(2).stadium(court)
                .startDate(today.minusDays(3)).endDate(today.minusDays(1))
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0))
                .build();
        Pageable pageable = PageRequest.of(0, 20);
        when(maintenanceScheduleRepository.findByStadiumStadiumIdOrderByStartDateDesc(court.getStadiumId(), pageable))
                .thenReturn(new PageImpl<>(List.of(allDayToday, endedYesterday)));

        Page<MaintenanceScheduleResponse> result = service.listSchedules(court.getStadiumId(), OWNER_USER_ID, pageable);

        assertTrue(result.getContent().get(0).getActive());
        assertFalse(result.getContent().get(1).getActive());
    }
}
