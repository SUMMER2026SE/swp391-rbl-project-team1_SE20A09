package com.sportvenue.service.impl;

import com.sportvenue.dto.booking.BookingDetailResponse;
import com.sportvenue.dto.request.AccessoryItem;
import com.sportvenue.dto.request.CreateBookingRequest;
import com.sportvenue.entity.Accessory;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.BookingAccessory;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.enums.TransactionStatus;
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
import com.sportvenue.service.AdminDashboardService;
import com.sportvenue.service.MaintenanceScheduleService;
import com.sportvenue.service.WalletService;
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
    @Mock private AdminDashboardService adminDashboardService;
    @Mock private WalletService walletService;

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
        // totalPrice = 150000 (slot) + 50000*2=100000 (accessory) + 10000 (service) = 260000
        assertEquals(0, new BigDecimal("260000.00").compareTo(response.getTotalPrice()),
                "totalPrice phải = slot + accessories + serviceFee (10k)");
        assertEquals(0, new BigDecimal("10000.00").compareTo(response.getServiceFee()),
                "serviceFee phải được tính động là 10k");

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

        // 150000 + 0 + 10000 = 160000
        assertEquals(0, new BigDecimal("160000.00").compareTo(response.getTotalPrice()));
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
    @DisplayName("confirmPayment: PENDING_PAYMENT + đúng user → CONFIRMED + AWAITING_CASH_PAYMENT + expiredAt cleared")
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
        assertEquals(PaymentStatus.AWAITING_CASH_PAYMENT, booking.getPaymentStatus(),
                "Cash confirm không được set PAID trực tiếp — tiền chưa qua cổng thanh toán nào (mục 1.5)");
        assertNull(booking.getExpiredAt(), "expiredAt phải bị clear sau khi confirm");
        assertEquals(50, response.getBookingId());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(com.sportvenue.entity.enums.PaymentMethod.CASH, savedPayment.getPaymentMethod());
        assertEquals(TransactionStatus.PENDING, savedPayment.getPaymentStatus());
        assertEquals(new BigDecimal("270000"), savedPayment.getAmount());
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
    @DisplayName("cancelBooking: customer cancels <12h before play time -> 0% refund, gateway skipped, no error")
    void cancelBooking_customerCancels_below12h_skipsGatewayAndSucceeds() {
        // Arrange: giờ chơi còn 5h nữa -> tiering trả về 0% (docs/qa_findings_refactor_plan.md mục 1.3)
        LocalDateTime targetPlayTime = LocalDateTime.now().plusHours(5);
        slot.setStartTime(targetPlayTime.toLocalTime());

        Booking booking = Booking.builder()
                .bookingId(100)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("150000"))
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .reservationDate(targetPlayTime.toLocalDate())
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
        // findRefundPaymentByBookingId được gọi 2 lần với 2 mục đích khác nhau trong cùng luồng:
        // lần 1 (trong processLocalCancellationTx) là guard chống double-refund — TRƯỚC khi hủy,
        // chưa có refund payment nào nên phải rỗng; lần 2 (trong toBookingDetailResponse) lấy
        // refund payment vừa tạo để hiển thị — lúc này mới có. Query dùng amount <= 0 nên PHẢI
        // tìm thấy payment 0đ ở lần 2 — đây chính là bug thực tế đã gặp: query cũ dùng "< 0" bỏ
        // sót refund 0đ, khiến trang chi tiết booking hiện "Đang xử lý..." mãi thay vì "0đ (0%)".
        when(paymentRepository.findRefundPaymentByBookingId(100))
                .thenReturn(List.of())
                .thenReturn(List.of(com.sportvenue.entity.Payment.builder()
                        .paymentId(2).amount(BigDecimal.ZERO)
                        .paymentStatus(com.sportvenue.entity.enums.TransactionStatus.SUCCESS)
                        .build()));

        // Act — không được throw BadRequestException("Lỗi hoàn tiền qua cổng thanh toán...")
        BookingDetailResponse response = bookingService.cancelBooking(principal, 100, "Bận đột xuất");

        // Assert
        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        verify(paymentService, never()).processRefund(any(), any(), any());
        verify(paymentRepository).save(org.mockito.ArgumentMatchers.argThat(
                p -> p.getAmount().compareTo(BigDecimal.ZERO) == 0));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getRefundedAmount()),
                "refundedAmount phải là 0đ, không được null (mới hiện đúng UI thay vì 'Đang xử lý...' mãi)");
        assertEquals(0, response.getRefundPercent());
    }

    @Test
    @DisplayName("cancelBooking: customer cancels a DEPOSITED booking >=24h before play -> still 0% (mất cọc, không phải lỗi tiering)")
    void cancelBooking_customerCancels_depositBooking_alwaysZeroRefund() {
        // Arrange: giờ chơi còn tận 5 ngày (>=24h, lẽ ra 100% nếu là thanh toán đầy đủ) — chứng
        // minh việc hoàn 0đ ở đây là do CHÍNH SÁCH đặt cọc (không hoàn khi khách tự hủy), không
        // phải do rơi vào tier <12h/12-24h.
        Booking booking = Booking.builder()
                .bookingId(100)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("150000"))
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.DEPOSITED)
                .reservationDate(futureDate())
                .build();

        com.sportvenue.entity.Payment depositPayment = com.sportvenue.entity.Payment.builder()
                .paymentId(1)
                .booking(booking)
                .amount(new BigDecimal("45000")) // 30% cọc
                .transactionCode("TXN_DEPOSIT")
                .paymentStatus(com.sportvenue.entity.enums.TransactionStatus.SUCCESS)
                .build();

        when(bookingRepository.findDetailById(100)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(100)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(depositPayment));
        when(paymentRepository.save(any(com.sportvenue.entity.Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of());

        BookingDetailResponse response = bookingService.cancelBooking(principal, 100, "Đổi ý");

        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        verify(paymentService, never()).processRefund(any(), any(), any());
        verify(paymentRepository).save(org.mockito.ArgumentMatchers.argThat(
                p -> p.getAmount().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    @DisplayName("cancelBooking: venue owner can cancel an UNPAID booking directly (nothing to refund) -> success")
    void cancelBooking_byVenueOwner_whenUnpaid_success() {
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

        when(bookingRepository.findByIdForUpdate(100)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.findDetailById(100)).thenReturn(Optional.of(booking));

        // Act — không có gì để hoàn (chưa thu tiền thật) nên Owner được hủy thẳng, không cần qua /refund
        BookingDetailResponse response = bookingService.cancelBooking(ownerPrincipal, 100, "Owner closed court");

        // Assert
        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals("Owner closed court", booking.getCancelReason());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("cancelBooking: venue owner CANNOT cancel an already-PAID booking -> Forbidden (mục 1.2 bypass fix)")
    void cancelBooking_byVenueOwner_whenPaid_throwsForbidden() {
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
                .bookingId(101)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("150000"))
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .reservationDate(futureDate())
                .build();

        UserPrincipal ownerPrincipal = new UserPrincipal(ownerUser);

        when(bookingRepository.findDetailById(101)).thenReturn(Optional.of(booking));

        // Act & Assert — Owner phải dùng /owner/bookings/{id}/refund thay vì né chính sách qua đây
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> bookingService.cancelBooking(ownerPrincipal, 101, "Owner tries to bypass refund policy"));
        assertTrue(ex.getMessage().contains("Hoàn tiền"));
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus(), "booking không được đổi trạng thái khi bị chặn");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking: venue owner cancelling a DEPOSITED booking is blocked too (same as PAID)")
    void cancelBooking_byVenueOwner_whenDeposited_throwsForbidden() {
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
                .bookingId(102)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("150000"))
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.DEPOSITED)
                .reservationDate(futureDate())
                .build();

        UserPrincipal ownerPrincipal = new UserPrincipal(ownerUser);
        when(bookingRepository.findDetailById(102)).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class,
                () -> bookingService.cancelBooking(ownerPrincipal, 102, "Owner tries again with deposit"));
        verify(bookingRepository, never()).save(any());
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
        when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of(pendingRefund));

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

        // Assert: priceOverride (200000) + serviceFee (10000) = 210000
        assertEquals(new BigDecimal("210000"), res.getTotalPrice());
    }

    @Test
    @DisplayName("calculateServiceFee: floor limit test")
    void calculateServiceFee_floorLimit() {
        // 5% of 100k = 5k < 10k -> should floor to 10k
        BigDecimal fee = BookingServiceImpl.calculateServiceFee(new BigDecimal("100000"));
        assertEquals(new BigDecimal("10000"), fee);
    }

    @Test
    @DisplayName("calculateServiceFee: normal percentage test")
    void calculateServiceFee_normalPercentage() {
        // 5% of 400k = 20k -> should be 20k
        BigDecimal fee = BookingServiceImpl.calculateServiceFee(new BigDecimal("400000"));
        assertEquals(new BigDecimal("20000"), fee);
    }

    @Test
    @DisplayName("calculateServiceFee: ceiling limit test")
    void calculateServiceFee_ceilingLimit() {
        // 5% of 800k = 40k > 30k -> should cap at 30k
        BigDecimal fee = BookingServiceImpl.calculateServiceFee(new BigDecimal("800000"));
        assertEquals(new BigDecimal("30000"), fee);
    }

    @Test
    @DisplayName("cancelBooking: Customer cancellation applies tiered refund and deducts service fee")
    void cancelBooking_customerRequest_appliesTieringAndDeductsServiceFee() {
        Booking booking = Booking.builder()
                .bookingId(100)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("150000"))
                .serviceFee(new BigDecimal("10000"))
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .reservationDate(LocalDate.now().plusDays(2)) // far future (>=24h)
                .build();

        com.sportvenue.entity.Payment successPayment = com.sportvenue.entity.Payment.builder()
                .paymentId(100)
                .amount(new BigDecimal("150000"))
                .paymentStatus(com.sportvenue.entity.enums.TransactionStatus.SUCCESS)
                .paymentMethod(com.sportvenue.entity.enums.PaymentMethod.VNPAY)
                .transactionCode("TX123")
                .build();

        org.mockito.Mockito.lenient().when(bookingRepository.findByIdForUpdate(100)).thenReturn(Optional.of(booking));
        org.mockito.Mockito.lenient().when(bookingRepository.findById(100)).thenReturn(Optional.of(booking));
        org.mockito.Mockito.lenient().when(bookingRepository.findDetailById(100)).thenReturn(Optional.of(booking));
        org.mockito.Mockito.lenient().when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(successPayment));
        org.mockito.Mockito.lenient().when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(paymentRepository.save(any(com.sportvenue.entity.Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingDetailResponse response = bookingService.cancelBooking(principal, 100, "I want to cancel");

        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals("I want to cancel", booking.getCancelReason());
        verify(paymentRepository).save(org.mockito.ArgumentMatchers.argThat(p -> p.getAmount().compareTo(new BigDecimal("-140000")) == 0));
    }

    @Test
    @DisplayName("cancelBooking: Customer cancellation less than 12 hours before play time -> 0% refund")
    void cancelBooking_customerRequest_appliesTieringAndDeductsServiceFee_below12h() {
        LocalDateTime targetPlayTime = LocalDateTime.now().plusHours(5);
        Booking booking = Booking.builder()
                .bookingId(100)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("150000"))
                .serviceFee(new BigDecimal("10000"))
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .reservationDate(targetPlayTime.toLocalDate())
                .build();
        slot.setStartTime(targetPlayTime.toLocalTime());

        com.sportvenue.entity.Payment successPayment = com.sportvenue.entity.Payment.builder()
                .paymentId(100)
                .amount(new BigDecimal("150000"))
                .paymentStatus(com.sportvenue.entity.enums.TransactionStatus.SUCCESS)
                .paymentMethod(com.sportvenue.entity.enums.PaymentMethod.VNPAY)
                .transactionCode("TX123")
                .build();

        org.mockito.Mockito.lenient().when(bookingRepository.findByIdForUpdate(100)).thenReturn(Optional.of(booking));
        org.mockito.Mockito.lenient().when(bookingRepository.findById(100)).thenReturn(Optional.of(booking));
        org.mockito.Mockito.lenient().when(bookingRepository.findDetailById(100)).thenReturn(Optional.of(booking));
        org.mockito.Mockito.lenient().when(paymentRepository.findSuccessPaymentsByBookingId(100)).thenReturn(List.of(successPayment));
        org.mockito.Mockito.lenient().when(paymentRepository.findRefundPaymentByBookingId(100)).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(paymentRepository.save(any(com.sportvenue.entity.Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingDetailResponse response = bookingService.cancelBooking(principal, 100, "Too late cancellation");

        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        verify(paymentRepository).save(org.mockito.ArgumentMatchers.argThat(p -> p.getAmount().compareTo(BigDecimal.ZERO) == 0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // confirmCashPaymentReceived — Owner xác nhận đã thu tiền mặt tại sân
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmCashPaymentReceived: Owner đúng sân + AWAITING_CASH_PAYMENT → PAID")
    void confirmCashPaymentReceived_happyPath() {
        User ownerUser = User.builder()
                .userId(99).email("owner@example.com")
                .role(Role.builder().roleName("Owner").build())
                .build();
        Owner owner = Owner.builder().ownerId(5).user(ownerUser).build();
        stadium.setOwner(owner);
        UserPrincipal ownerPrincipal = new UserPrincipal(ownerUser);

        Booking booking = Booking.builder()
                .bookingId(200)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("270000"))
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.AWAITING_CASH_PAYMENT)
                .build();

        Payment cashPayment = Payment.builder()
                .paymentId(10)
                .booking(booking)
                .paymentMethod(com.sportvenue.entity.enums.PaymentMethod.CASH)
                .amount(new BigDecimal("270000"))
                .transactionCode("CASH_200")
                .paymentStatus(TransactionStatus.PENDING)
                .build();

        when(bookingRepository.findByIdForUpdate(200)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findCashPaymentsByBookingIdAndMethodAndStatus(
                200, com.sportvenue.entity.enums.PaymentMethod.CASH, TransactionStatus.PENDING))
                .thenReturn(List.of(cashPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDetailResponse response = bookingService.confirmCashPaymentReceived(ownerPrincipal, 200);

        assertEquals(PaymentStatus.PAID, booking.getPaymentStatus());
        assertEquals(TransactionStatus.SUCCESS, cashPayment.getPaymentStatus());
        assertNotNull(cashPayment.getPaidAt());
        assertEquals(200, response.getBookingId());
        verify(adminDashboardService).evictDashboardCache();
    }

    @Test
    @DisplayName("confirmCashPaymentReceived: gọi lại trên đơn đã PAID → idempotent, không lỗi, không đổi state")
    void confirmCashPaymentReceived_alreadyPaid_isIdempotent() {
        User ownerUser = User.builder()
                .userId(99).email("owner@example.com")
                .role(Role.builder().roleName("Owner").build())
                .build();
        Owner owner = Owner.builder().ownerId(5).user(ownerUser).build();
        stadium.setOwner(owner);
        UserPrincipal ownerPrincipal = new UserPrincipal(ownerUser);

        Booking booking = Booking.builder()
                .bookingId(201)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("270000"))
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        when(bookingRepository.findByIdForUpdate(201)).thenReturn(Optional.of(booking));

        BookingDetailResponse response = bookingService.confirmCashPaymentReceived(ownerPrincipal, 201);

        assertEquals(PaymentStatus.PAID, booking.getPaymentStatus());
        assertEquals(201, response.getBookingId());
        verify(bookingRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
        verify(adminDashboardService, never()).evictDashboardCache();
    }

    @Test
    @DisplayName("confirmCashPaymentReceived: Customer (không phải Owner) gọi → ForbiddenException")
    void confirmCashPaymentReceived_calledByCustomer_throwsForbidden() {
        Booking booking = Booking.builder()
                .bookingId(202)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.AWAITING_CASH_PAYMENT)
                .build();
        // stadium chưa gán owner nào -> resolveOwner() trả null -> chắc chắn Forbidden dù ai gọi
        when(bookingRepository.findByIdForUpdate(202)).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class,
                () -> bookingService.confirmCashPaymentReceived(principal, 202));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmCashPaymentReceived: Owner của sân khác gọi → ForbiddenException")
    void confirmCashPaymentReceived_calledByOtherOwner_throwsForbidden() {
        User realOwnerUser = User.builder().userId(99).role(Role.builder().roleName("Owner").build()).build();
        Owner realOwner = Owner.builder().ownerId(5).user(realOwnerUser).build();
        stadium.setOwner(realOwner);

        User otherOwnerUser = User.builder().userId(77).role(Role.builder().roleName("Owner").build()).build();
        UserPrincipal otherOwnerPrincipal = new UserPrincipal(otherOwnerUser);

        Booking booking = Booking.builder()
                .bookingId(203)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.AWAITING_CASH_PAYMENT)
                .build();
        when(bookingRepository.findByIdForUpdate(203)).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class,
                () -> bookingService.confirmCashPaymentReceived(otherOwnerPrincipal, 203));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmCashPaymentReceived: booking không phải AWAITING_CASH_PAYMENT → BadRequestException")
    void confirmCashPaymentReceived_wrongPaymentStatus_throwsBadRequest() {
        User ownerUser = User.builder().userId(99).role(Role.builder().roleName("Owner").build()).build();
        Owner owner = Owner.builder().ownerId(5).user(ownerUser).build();
        stadium.setOwner(owner);
        UserPrincipal ownerPrincipal = new UserPrincipal(ownerUser);

        Booking booking = Booking.builder()
                .bookingId(204)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
        when(bookingRepository.findByIdForUpdate(204)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class,
                () -> bookingService.confirmCashPaymentReceived(ownerPrincipal, 204));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmCashPaymentReceived: booking đã CANCELLED → BadRequestException")
    void confirmCashPaymentReceived_cancelledBooking_throwsBadRequest() {
        User ownerUser = User.builder().userId(99).role(Role.builder().roleName("Owner").build()).build();
        Owner owner = Owner.builder().ownerId(5).user(ownerUser).build();
        stadium.setOwner(owner);
        UserPrincipal ownerPrincipal = new UserPrincipal(ownerUser);

        Booking booking = Booking.builder()
                .bookingId(205)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .bookingStatus(BookingStatus.CANCELLED)
                .paymentStatus(PaymentStatus.AWAITING_CASH_PAYMENT)
                .build();
        when(bookingRepository.findByIdForUpdate(205)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class,
                () -> bookingService.confirmCashPaymentReceived(ownerPrincipal, 205));
        verify(bookingRepository, never()).save(any());
    }

}

