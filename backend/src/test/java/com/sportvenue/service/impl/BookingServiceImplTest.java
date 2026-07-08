package com.sportvenue.service.impl;

import com.sportvenue.dto.booking.BookingDetailResponse;
import com.sportvenue.dto.request.AccessoryItem;
import com.sportvenue.dto.request.CreateBookingRequest;
import com.sportvenue.entity.Accessory;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.BookingAccessory;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.TimeSlotException;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.DuplicateResourceException;
import com.sportvenue.exception.ForbiddenException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.AccessoryRepository;
import com.sportvenue.repository.BookingAccessoryRepository;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.repository.TimeSlotExceptionRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.MaintenanceScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.sportvenue.entity.StadiumImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;
import com.sportvenue.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC-CUS-01: Unit test cho {@link BookingServiceImpl} — happy path + edge cases.
 *
 * <p>Toàn bộ repository mock qua {@code @Mock}; chỉ service {@code BookingServiceImpl}
 * được {@code @InjectMocks}. Tập trung vào {@code createBooking} và {@code confirmPayment}.</p>
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private StadiumRepository stadiumRepository;
    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private AccessoryRepository accessoryRepository;
    @Mock private BookingAccessoryRepository bookingAccessoryRepository;
    @Mock private TimeSlotExceptionRepository timeSlotExceptionRepository;
    @Mock private com.sportvenue.service.MaintenanceScheduleService maintenanceScheduleService;
    @Mock private PaymentService paymentService;
    @Mock private TransactionTemplate transactionTemplate;

    @Mock private com.sportvenue.service.EmailService emailService;
    @Mock private com.sportvenue.service.NotificationService notificationService;
    @Mock private com.sportvenue.util.AfterCommitExecutor afterCommitExecutor;

    @InjectMocks private BookingServiceImpl bookingService;

    private User customer;
    private Stadium stadium;
    private TimeSlot slot;
    private UserPrincipal principal;
    private Accessory accessory;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .userId(1)
                .email("khach@example.com")
                .firstName("An")
                .lastName("Nguyen")
                .role(Role.builder().roleName("Customer").build())
                .build();

        stadium = Stadium.builder()
                .stadiumId(10)
                .stadiumName("Sân A")
                .pricePerHour(new BigDecimal("100000"))
                .build();

        slot = TimeSlot.builder()
                .slotId(20)
                .stadium(stadium)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .pricePerSlot(new BigDecimal("150000"))
                .slotStatus(SlotStatus.AVAILABLE)
                .build();

        principal = new UserPrincipal(customer);

        accessory = Accessory.builder()
                .accessoryId(30)
                .stadium(stadium)
                .name("Bóng đá")
                .pricePerUnit(new BigDecimal("50000"))
                .quantity(20)
                .isAvailable(true)
                .build();

        org.mockito.Mockito.lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
        org.mockito.Mockito.lenient().when(bookingRepository.findByIdForUpdate(any(Integer.class)))
                .thenAnswer(invocation -> bookingRepository.findDetailById(invocation.getArgument(0)));
    }

    /** Helper: reservationDate 7 ngày trong tương lai → luôn future. */
    private LocalDate futureDate() {
        return LocalDate.now().plusDays(7);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Happy path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBooking: happy path — totalPrice = slot + accessories + service fee")
    void createBooking_happyPath_includesAccessoriesAndServiceFee() {
        // Arrange
        LocalDate resDate = futureDate();
        CreateBookingRequest request = CreateBookingRequest.builder()
                .stadiumId(stadium.getStadiumId())
                .slotId(slot.getSlotId())
                .reservationDate(resDate)
                .note("Test")
                .accessories(List.of(
                        AccessoryItem.builder().accessoryId(30).quantity(2).build()))
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByIdForUpdate(20)).thenReturn(Optional.of(slot));
        when(bookingRepository.existsActiveBooking(
                any(), any(), any(), anyList())).thenReturn(false);
        when(accessoryRepository.findById(30)).thenReturn(Optional.of(accessory));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setBookingId(99);
            return b;
        });

        // Act
        BookingDetailResponse response = bookingService.createBooking(principal, request);

        // Assert
        assertNotNull(response);
        assertEquals(99, response.getBookingId());
        // totalPrice = 150000 (slot) + 50000*2=100000 (accessory) + 20000 (service) = 270000
        assertEquals(0, new BigDecimal("270000.00").compareTo(response.getTotalPrice()),
                "totalPrice phải = slot + accessories + serviceFee (20k)");

        // Verify accessory đã được persist với bookingId
        ArgumentCaptor<List<BookingAccessory>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookingAccessoryRepository).saveAll(captor.capture());
        List<BookingAccessory> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals(30, saved.get(0).getAccessoryId());
        assertEquals(2, saved.get(0).getQuantity());
        assertEquals(0, new BigDecimal("50000.00").compareTo(saved.get(0).getUnitPrice()));

        // Verify booking được set PENDING_PAYMENT + expiredAt = now + 5min
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking persisted = bookingCaptor.getValue();
        assertEquals(BookingStatus.PENDING_PAYMENT, persisted.getBookingStatus());
        assertEquals(PaymentStatus.UNPAID, persisted.getPaymentStatus());
        assertNotNull(persisted.getExpiredAt(), "expiredAt phải được set");
        assertTrue(persisted.getExpiredAt().isAfter(LocalDateTime.now().plusMinutes(4)),
                "expiredAt phải sau now + 4 phút");
    }

    @Test
    @DisplayName("createBooking: không có accessories → totalPrice = slot + service fee")
    void createBooking_noAccessories_onlySlotPlusServiceFee() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .stadiumId(stadium.getStadiumId())
                .slotId(slot.getSlotId())
                .reservationDate(futureDate())
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByIdForUpdate(20)).thenReturn(Optional.of(slot));
        when(bookingRepository.existsActiveBooking(
                any(), any(), any(), anyList())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setBookingId(100);
            return b;
        });

        BookingDetailResponse response = bookingService.createBooking(principal, request);

        // 150000 + 0 + 20000 = 170000
        assertEquals(0, new BigDecimal("170000.00").compareTo(response.getTotalPrice()));
        verify(bookingAccessoryRepository, never()).saveAll(anyList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Conflict & validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBooking: slot đã được đặt active → DuplicateResourceException")
    void createBooking_slotAlreadyBooked_throwsConflict() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .stadiumId(10).slotId(20).reservationDate(futureDate()).build();

        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByIdForUpdate(20)).thenReturn(Optional.of(slot));
        when(bookingRepository.existsActiveBooking(
                any(), any(), any(), anyList())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> bookingService.createBooking(principal, request));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking: slot trong quá khứ → BadRequestException")
    void createBooking_pastSlot_throwsBadRequest() {
        // reservation_date = hôm nay, slot bắt đầu từ sáng → đã qua
        slot.setStartTime(LocalTime.of(0, 1));
        CreateBookingRequest request = CreateBookingRequest.builder()
                .stadiumId(10).slotId(20).reservationDate(LocalDate.now()).build();

        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByIdForUpdate(20)).thenReturn(Optional.of(slot));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(principal, request));
        assertTrue(ex.getMessage().contains("quá khứ"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking: slot MAINTENANCE → BadRequestException")
    void createBooking_maintenanceSlot_throwsBadRequest() {
        slot.setSlotStatus(SlotStatus.MAINTENANCE);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .stadiumId(10).slotId(20).reservationDate(futureDate()).build();

        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByIdForUpdate(20)).thenReturn(Optional.of(slot));

        assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(principal, request));
    }

    @Test
    @DisplayName("createBooking: slot đang bảo trì (isSlotUnderMaintenance=true) → BadRequestException")
    void createBooking_stadiumUnderMaintenance_throwsBadRequest() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .stadiumId(10).slotId(20).reservationDate(futureDate()).build();

        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByIdForUpdate(20)).thenReturn(Optional.of(slot));
        when(maintenanceScheduleService.isSlotUnderMaintenance(eq(stadium), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(principal, request));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking: slot không thuộc sân → BadRequestException")
    void createBooking_slotNotInStadium_throwsBadRequest() {
        Stadium otherStadium = Stadium.builder().stadiumId(99).build();
        slot.setStadium(otherStadium);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .stadiumId(10).slotId(20).reservationDate(futureDate()).build();

        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByIdForUpdate(20)).thenReturn(Optional.of(slot));

        assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(principal, request));
    }

    @Test
    @DisplayName("createBooking: phụ kiện không tồn tại → ResourceNotFoundException")
    void createBooking_accessoryNotFound_throwsNotFound() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .stadiumId(10).slotId(20).reservationDate(futureDate())
                .accessories(List.of(AccessoryItem.builder().accessoryId(999).quantity(1).build()))
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByIdForUpdate(20)).thenReturn(Optional.of(slot));
        when(bookingRepository.existsActiveBooking(
                any(), any(), any(), anyList())).thenReturn(false);
        when(accessoryRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(principal, request));
    }

    @Test
    @DisplayName("createBooking: phụ kiện isAvailable=false → BadRequestException")
    void createBooking_accessoryUnavailable_throwsBadRequest() {
        accessory.setIsAvailable(false);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .stadiumId(10).slotId(20).reservationDate(futureDate())
                .accessories(List.of(AccessoryItem.builder().accessoryId(30).quantity(1).build()))
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByIdForUpdate(20)).thenReturn(Optional.of(slot));
        when(bookingRepository.existsActiveBooking(
                any(), any(), any(), anyList())).thenReturn(false);
        when(accessoryRepository.findById(30)).thenReturn(Optional.of(accessory));

        assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(principal, request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // confirmPayment
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmPayment: PENDING_PAYMENT + đúng user → CONFIRMED + expiredAt cleared")
    void confirmPayment_happyPath() {
        Booking booking = Booking.builder()
                .bookingId(50)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("270000"))
                .bookingStatus(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.UNPAID)
                .reservationDate(futureDate())
                .expiredAt(LocalDateTime.now().plusMinutes(3))
                .build();

        when(bookingRepository.findDetailById(50)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDetailResponse response = bookingService.confirmPayment(principal, 50);

        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(PaymentStatus.PAID, booking.getPaymentStatus());
        assertNull(booking.getExpiredAt(), "expiredAt phải bị clear sau khi confirm");
        assertEquals(50, response.getBookingId());
    }

    @Test
    @DisplayName("confirmPayment: booking không tồn tại → ResourceNotFoundException")
    void confirmPayment_notFound_throws() {
        when(bookingRepository.findDetailById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.confirmPayment(principal, 99));
    }

    @Test
    @DisplayName("confirmPayment: sai user → BadRequestException (không phải 403 raw vì service throw BadRequest)")
    void confirmPayment_wrongUser_throwsForbidden() {
        User other = User.builder().userId(999).role(Role.builder().roleName("Customer").build()).build();
        Booking booking = Booking.builder()
                .bookingId(50).user(other).stadium(stadium).slot(slot)
                .bookingStatus(BookingStatus.PENDING_PAYMENT).build();
        when(bookingRepository.findDetailById(50)).thenReturn(Optional.of(booking));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bookingService.confirmPayment(principal, 50));
        assertTrue(ex.getMessage().contains("quyền"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmPayment: booking đã CANCELLED → BadRequestException")
    void confirmPayment_cancelledBooking_throws() {
        Booking booking = Booking.builder()
                .bookingId(50).user(customer).stadium(stadium).slot(slot)
                .bookingStatus(BookingStatus.CANCELLED).build();
        when(bookingRepository.findDetailById(50)).thenReturn(Optional.of(booking));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bookingService.confirmPayment(principal, 50));
        assertTrue(ex.getMessage().contains("huỷ"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getSlotsByDate / getWeeklySlots smoke tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSlotsByDate: trả về list có cờ available")
    void getSlotsByDate_marksAvailableCorrectly() {
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByStadiumStadiumIdAndSlotStatus(10, SlotStatus.AVAILABLE))
                .thenReturn(List.of(slot));
        when(bookingRepository.findBookedSlotIds(eq(10), any(LocalDate.class), anyList()))
                .thenReturn(List.of());

        var result = bookingService.getSlotsByDate(10, futureDate());
        assertEquals(1, result.size());
        assertEquals(20, result.get(0).getSlotId());
        assertTrue(result.get(0).getAvailable());
    }

    @Test
    @DisplayName("getWeeklySlots: snap về thứ 2 — weekStart là thứ 4 → trả thứ 2")
    void getWeeklySlots_snapToMonday() {
        LocalDate wednesday = LocalDate.of(2026, 6, 24); // Wednesday
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByStadiumStadiumIdAndSlotStatus(10, SlotStatus.AVAILABLE))
                .thenReturn(List.of(slot));
        when(bookingRepository.findWeeklyBookings(eq(10), any(LocalDate.class), any(LocalDate.class), anyList()))
                .thenReturn(List.of());

        var result = bookingService.getWeeklySlots(10, wednesday);
        // Snap Monday = 2026-06-22
        assertEquals("2026-06-22", result.getWeekStart());
        assertEquals("2026-06-28", result.getWeekEnd());
        assertEquals(7, result.getDays().size());
    }

    @Test
    @DisplayName("getWeeklySlots: PENDING_PAYMENT hiển thị HELD cùng hạn giữ chỗ")
    void getWeeklySlots_pendingPaymentDisplaysHeld() {
        LocalDate reservationDate = futureDate();
        LocalDateTime heldUntil = LocalDateTime.now().plusMinutes(5).withNano(0);
        Booking heldBooking = Booking.builder()
                .bookingId(50)
                .stadium(stadium)
                .slot(slot)
                .user(customer)
                .reservationDate(reservationDate)
                .bookingStatus(BookingStatus.PENDING_PAYMENT)
                .expiredAt(heldUntil)
                .build();

        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByStadiumStadiumIdAndSlotStatus(10, SlotStatus.AVAILABLE))
                .thenReturn(List.of(slot));
        when(bookingRepository.findWeeklyBookings(eq(10), any(LocalDate.class), any(LocalDate.class), anyList()))
                .thenReturn(List.of(heldBooking));

        var result = bookingService.getWeeklySlots(10, reservationDate);
        int dayIndex = reservationDate.getDayOfWeek().getValue() - 1;
        var heldSlot = result.getDays().get(dayIndex).getSlots().get(0);

        assertEquals("HELD", heldSlot.getStatus());
        assertEquals(heldUntil.toString(), heldSlot.getHeldUntil());
    }

    @Test
    @DisplayName("getWeeklySlots: sân đang bảo trì → slot tương lai được đánh dấu MAINTENANCE")
    void getWeeklySlots_stadiumUnderMaintenance_marksFutureSlotsAsMaintenance() {
        LocalDate weekStart = LocalDate.now().plusWeeks(2);
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByStadiumStadiumIdAndSlotStatus(10, SlotStatus.AVAILABLE))
                .thenReturn(List.of(slot));
        when(bookingRepository.findWeeklyBookings(eq(10), any(LocalDate.class), any(LocalDate.class), anyList()))
                .thenReturn(List.of());
        when(maintenanceScheduleService.getDayMaintenanceForDateRange(eq(stadium), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> allDaysInRangeMappedTo(invocation, true));

        var result = bookingService.getWeeklySlots(10, weekStart);

        result.getDays().forEach(day -> day.getSlots().forEach(item ->
                assertEquals("MAINTENANCE", item.getStatus(),
                        "Ngày " + day.getDate() + " phải là MAINTENANCE khi cả ngày đang bảo trì")));
    }

    @Test
    @DisplayName("getWeeklySlots: không bảo trì → slot tương lai vẫn AVAILABLE")
    void getWeeklySlots_notUnderMaintenance_slotsStayAvailable() {
        LocalDate weekStart = LocalDate.now().plusWeeks(2);
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByStadiumStadiumIdAndSlotStatus(10, SlotStatus.AVAILABLE))
                .thenReturn(List.of(slot));
        when(bookingRepository.findWeeklyBookings(eq(10), any(LocalDate.class), any(LocalDate.class), anyList()))
                .thenReturn(List.of());
        when(maintenanceScheduleService.getDayMaintenanceForDateRange(eq(stadium), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> allDaysInRangeMappedTo(invocation, false));

        var result = bookingService.getWeeklySlots(10, weekStart);

        result.getDays().forEach(day -> day.getSlots().forEach(item ->
                assertEquals("AVAILABLE", item.getStatus())));
    }

    /**
     * Helper: build 1 {@code Map<LocalDate, DayMaintenance>} phủ đúng [rangeStart, rangeEnd] được
     * truyền vào mock — dùng cho stub {@code getDayMaintenanceForDateRange}. {@code value=true} ->
     * cả ngày bảo trì ({@link MaintenanceScheduleService.DayMaintenance#ALL_DAY}).
     */
    private static Map<LocalDate, MaintenanceScheduleService.DayMaintenance> allDaysInRangeMappedTo(
            org.mockito.invocation.InvocationOnMock invocation, boolean value) {
        LocalDate rangeStart = invocation.getArgument(1);
        LocalDate rangeEnd = invocation.getArgument(2);
        Map<LocalDate, MaintenanceScheduleService.DayMaintenance> map = new java.util.LinkedHashMap<>();
        MaintenanceScheduleService.DayMaintenance dayMaintenance = value
                ? MaintenanceScheduleService.DayMaintenance.ALL_DAY
                : MaintenanceScheduleService.DayMaintenance.NONE;
        for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
            map.put(d, dayMaintenance);
        }
        return map;
    }

    @Test
    @DisplayName("getMyBookings: không filter → lấy tất cả status")
    void getMyBookings_noFilter_usesAllStatusQuery() {
        when(bookingRepository.findByUserUserIdOrderByReservationDateDesc(eq(1), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        var result = bookingService.getMyBookings(principal, 0, 10, null);
        assertNotNull(result);
        verify(bookingRepository, times(1))
                .findByUserUserIdOrderByReservationDateDesc(eq(1), any());
        verify(bookingRepository, never())
                .findByUserUserIdAndBookingStatusInOrderByReservationDateDesc(any(), any(), any());
    }

    @Test
    @DisplayName("getMyBookings: filter 'upcoming' → chỉ PENDING/CONFIRMED")
    void getMyBookings_upcomingFilter_usesStatusInQuery() {
        when(bookingRepository.findByUserUserIdAndBookingStatusInOrderByReservationDateDesc(
                eq(1), anyList(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        bookingService.getMyBookings(principal, 0, 10, "upcoming");
        verify(bookingRepository, times(1))
                .findByUserUserIdAndBookingStatusInOrderByReservationDateDesc(eq(1), anyList(), any());
    }

    @Test
    @DisplayName("getMyBookings: map booking → history item (toHistoryItemDto coverage)")
    void getMyBookings_mapsBookingToHistoryItem() {
        StadiumImage img = new StadiumImage();
        img.setImageUrl("https://example.com/s.jpg");
        stadium.setImages(new HashSet<>(List.of(img)));

        Booking b = Booking.builder()
                .bookingId(7)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("170000"))
                .bookingStatus(BookingStatus.CONFIRMED)
                .reservationDate(futureDate())
                .build();

        Page<Booking> page = new PageImpl<>(List.of(b), PageRequest.of(0, 10), 1);
        when(bookingRepository.findByUserUserIdOrderByReservationDateDesc(eq(1), any(Pageable.class)))
                .thenReturn(page);

        var result = bookingService.getMyBookings(principal, 0, 10, "all");
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        var item = result.getContent().get(0);
        assertEquals("BK000007", item.getDisplayId());
        assertEquals("Sân A", item.getVenue());
        assertEquals("170000", item.getPrice().toPlainString());
        assertEquals("confirmed", item.getStatus());
    }

    @Test
    @DisplayName("getMyBookings: filter 'completed' / 'cancelled' / 'pending' / 'confirmed' đều hoạt động")
    void getMyBookings_allFilterBranches() {
        for (String filter : new String[]{"completed", "cancelled", "pending", "confirmed"}) {
            when(bookingRepository.findByUserUserIdAndBookingStatusInOrderByReservationDateDesc(
                    eq(1), anyList(), any())).thenReturn(Page.empty());
            bookingService.getMyBookings(principal, 0, 10, filter);
        }
        // 4 filters × 1 call each
        verify(bookingRepository, times(4))
                .findByUserUserIdAndBookingStatusInOrderByReservationDateDesc(eq(1), anyList(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // cancelBooking Tests (UC-CUS-03)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelBooking: customer cancels their own booking successfully")
    void cancelBooking_happyPath_customerSuccess() {
        // Arrange
        Booking booking = Booking.builder()
                .bookingId(100)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("150000"))
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .reservationDate(futureDate())
                .build();

        com.sportvenue.entity.Payment originalPayment = com.sportvenue.entity.Payment.builder()
                .paymentId(1)
                .booking(booking)
                .amount(new BigDecimal("150000"))
                .transactionCode("TXN123")
                .paymentStatus(com.sportvenue.entity.enums.TransactionStatus.SUCCESS)
                .build();

        when(bookingRepository.findDetailById(100)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(100)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(originalPayment));
        when(paymentRepository.save(any(com.sportvenue.entity.Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BookingDetailResponse response = bookingService.cancelBooking(principal, 100, "Customer cancelled due to rain");

        // Assert
        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        assertEquals("Customer cancelled due to rain", booking.getCancelReason());
        assertNull(booking.getExpiredAt());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("cancelBooking: venue owner can cancel booking successfully -> success")
    void cancelBooking_byVenueOwner_success() {
        // Arrange
        User ownerUser = User.builder()
                .userId(99)
                .email("owner@example.com")
                .role(Role.builder().roleName("Owner").build())
                .build();
        Owner owner = Owner.builder()
                .ownerId(5)
                .user(ownerUser)
                .build();
        stadium.setOwner(owner);

        Booking booking = Booking.builder()
                .bookingId(100)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("150000"))
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .reservationDate(futureDate())
                .build();

        UserPrincipal ownerPrincipal = new UserPrincipal(ownerUser);

        when(bookingRepository.findDetailById(100)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BookingDetailResponse response = bookingService.cancelBooking(ownerPrincipal, 100, "Owner closed court");
        
        // Assert
        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals("Owner closed court", booking.getCancelReason());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("cancelBooking: gateway fails -> throws exception and keeps booking intact")
    void cancelBooking_gatewayFails_keepsBookingIntact() {
        // Arrange
        Booking booking = Booking.builder()
                .bookingId(100)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("150000"))
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .reservationDate(futureDate())
                .build();
        
        com.sportvenue.entity.Payment originalPayment = com.sportvenue.entity.Payment.builder()
                .paymentId(1)
                .booking(booking)
                .amount(new BigDecimal("150000"))
                .transactionCode("TXN123")
                .paymentStatus(com.sportvenue.entity.enums.TransactionStatus.SUCCESS)
                .build();

        when(bookingRepository.findDetailById(100)).thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(originalPayment));
        when(paymentRepository.save(any(com.sportvenue.entity.Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        
        org.mockito.Mockito.doThrow(new RuntimeException("Gateway error"))
                .when(paymentService).processRefund(any(), any(), any());

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, 
                () -> bookingService.cancelBooking(principal, 100, "Customer cancelled"));
        
        assertTrue(ex.getMessage().contains("Giao dịch hủy đơn thất bại"));
        // Confirm booking state is untouched
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(PaymentStatus.PAID, booking.getPaymentStatus());
        // verify save on booking was NEVER called in Tx1 (since it was paid) and Tx2 didn't happen for success
        verify(bookingRepository, never()).save(booking);
    }

    @Test
    @DisplayName("cancelBooking: user has no permission to cancel booking -> throws ForbiddenException")
    void cancelBooking_byOtherUser_throwsForbiddenException() {
        // Arrange
        User stranger = User.builder()
                .userId(888)
                .email("stranger@example.com")
                .role(Role.builder().roleName("Customer").build())
                .build();
        UserPrincipal strangerPrincipal = new UserPrincipal(stranger);

        Booking booking = Booking.builder()
                .bookingId(100)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .bookingStatus(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findDetailById(100)).thenReturn(Optional.of(booking));

        // Act & Assert
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> bookingService.cancelBooking(strangerPrincipal, 100, "No right"));
        assertTrue(ex.getMessage().contains("không có quyền"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking: booking already completed -> throws BadRequestException")
    void cancelBooking_alreadyCompleted_throwsBadRequest() {
        // Arrange
        Booking booking = Booking.builder()
                .bookingId(100)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .bookingStatus(BookingStatus.COMPLETED)
                .build();

        when(bookingRepository.findDetailById(100)).thenReturn(Optional.of(booking));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bookingService.cancelBooking(principal, 100, "Too late"));
        assertTrue(ex.getMessage().contains("Không thể hủy"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking: booking not found -> throws ResourceNotFoundException")
    void cancelBooking_notFound_throwsNotFound() {
        // Arrange
        when(bookingRepository.findDetailById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.cancelBooking(principal, 999, "Not exists"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking: refund already pending -> throws BadRequestException")
    void cancelBooking_refundAlreadyPending_throwsBadRequest() {
        // Arrange
        Booking booking = Booking.builder()
                .bookingId(100)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        com.sportvenue.entity.Payment pendingRefund = com.sportvenue.entity.Payment.builder()
                .paymentId(2)
                .booking(booking)
                .amount(new BigDecimal("-150000"))
                .paymentStatus(com.sportvenue.entity.enums.TransactionStatus.PENDING)
                .build();

        when(bookingRepository.findDetailById(100)).thenReturn(Optional.of(booking));
        when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(Optional.of(pendingRefund));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bookingService.cancelBooking(principal, 100, "Duplicate cancel"));
        assertTrue(ex.getMessage().contains("đang được xử lý hoặc đã thành công"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking: slot closed by exception -> throws BadRequestException")
    void createBooking_slotClosedByException_throwsBadRequest() {
        // Arrange
        CreateBookingRequest req = CreateBookingRequest.builder()
                .stadiumId(stadium.getStadiumId())
                .slotId(slot.getSlotId())
                .reservationDate(futureDate())
                .accessories(List.of())
                .build();

        TimeSlotException exception = TimeSlotException.builder()
                .slot(slot)
                .exceptionDate(req.getReservationDate())
                .closed(true)
                .build();

        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.of(customer));
        when(stadiumRepository.findById(stadium.getStadiumId())).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByIdForUpdate(slot.getSlotId())).thenReturn(Optional.of(slot));
        when(bookingRepository.existsActiveBooking(any(), any(), any(), any())).thenReturn(false);
        when(timeSlotExceptionRepository.findBySlotSlotIdAndExceptionDate(slot.getSlotId(), req.getReservationDate())).thenReturn(Optional.of(exception));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(principal, req));
        assertTrue(ex.getMessage().contains("tạm đóng"));
    }

    @Test
    @DisplayName("createBooking: slot price overridden by exception -> calculates correct totalPrice")
    void createBooking_slotPriceOverridden_calculatesCorrectTotalPrice() {
        // Arrange
        CreateBookingRequest req = CreateBookingRequest.builder()
                .stadiumId(stadium.getStadiumId())
                .slotId(slot.getSlotId())
                .reservationDate(futureDate())
                .accessories(List.of())
                .build();

        TimeSlotException exception = TimeSlotException.builder()
                .slot(slot)
                .exceptionDate(req.getReservationDate())
                .closed(false)
                .priceOverride(new BigDecimal("200000"))
                .build();

        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.of(customer));
        when(stadiumRepository.findById(stadium.getStadiumId())).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findByIdForUpdate(slot.getSlotId())).thenReturn(Optional.of(slot));
        when(bookingRepository.existsActiveBooking(any(), any(), any(), any())).thenReturn(false);
        when(timeSlotExceptionRepository.findBySlotSlotIdAndExceptionDate(slot.getSlotId(), req.getReservationDate())).thenReturn(Optional.of(exception));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setBookingId(200);
            return b;
        });

        // Act
        BookingDetailResponse res = bookingService.createBooking(principal, req);

        // Assert: priceOverride (200000) + serviceFee (20000) = 220000
        assertEquals(new BigDecimal("220000"), res.getTotalPrice());
    }
}
