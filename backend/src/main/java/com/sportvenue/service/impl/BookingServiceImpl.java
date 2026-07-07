package com.sportvenue.service.impl;

import com.sportvenue.dto.booking.BookingDetailResponse;
import com.sportvenue.dto.booking.BookingHistoryItemDto;
import com.sportvenue.dto.request.AccessoryItem;
import com.sportvenue.dto.request.CreateBookingRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.dto.response.WeeklySlotDayDto;
import com.sportvenue.dto.response.WeeklySlotItemDto;
import com.sportvenue.dto.response.WeeklySlotResponse;
import com.sportvenue.entity.Accessory;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.BookingAccessory;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumImage;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.DuplicateResourceException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.entity.Payment;
import com.sportvenue.entity.enums.TransactionStatus;
import com.sportvenue.repository.AccessoryRepository;
import com.sportvenue.repository.BookingAccessoryRepository;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.repository.TimeSlotExceptionRepository;
import com.sportvenue.entity.TimeSlotException;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.BookingService;
import com.sportvenue.service.MaintenanceScheduleService;
import com.sportvenue.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * UC-CUS-01: Triển khai single booking cho Customer.
 *
 * Quy tắc availability (xem thêm {@link #isSlotAvailable}):
 * <ul>
 *   <li>Slot unavailable nếu đã có booking PENDING/CONFIRMED cho cùng
 *       (stadiumId, slotId, reservationDate).</li>
 *   <li>Slot unavailable nếu (reservationDate + slot.startTime) đã qua so với hiện tại.</li>
 *   <li>Slot unavailable nếu {@code slotStatus != AVAILABLE} (MAINTENANCE / BOOKED).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    /** Trạng thái booking chiếm chỗ slot — dùng cho conflict detection. */
    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.PENDING, BookingStatus.CONFIRMED);

    /** Phí dịch vụ cố định (VNĐ) — server-side only, KHÔNG nhận từ client. */
    private static final BigDecimal SERVICE_FEE = new BigDecimal("20000");

    /** UC-CUS-01: Booking mới tạo được giữ 5 phút chờ thanh toán. */
    private static final long PAYMENT_HOLD_MINUTES = 5L;

    private final UserRepository userRepository;
    private final StadiumRepository stadiumRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final AccessoryRepository accessoryRepository;
    private final BookingAccessoryRepository bookingAccessoryRepository;
    private final TimeSlotExceptionRepository timeSlotExceptionRepository;
    private final MaintenanceScheduleService maintenanceScheduleService;
    private final PaymentService paymentService;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public BookingDetailResponse createBooking(UserPrincipal principal, CreateBookingRequest request) {
        User customer = userRepository.findById(principal.getUser().getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Stadium stadium = stadiumRepository.findById(request.getStadiumId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sân với ID " + request.getStadiumId()));

        // UC-CUS-01: Pessimistic Write lock trên slot row — 2 request đồng thời cho
        // cùng (stadium, slot, date) sẽ serialize qua bước conflict-check + insert,
        // kết hợp với partial unique index V5.5 để chặn double-booking ở tầng DB.
        TimeSlot slot = timeSlotRepository.findByIdForUpdate(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy khung giờ với ID " + request.getSlotId()));

        Optional<TimeSlotException> exceptionOpt = timeSlotExceptionRepository.findBySlotSlotIdAndExceptionDate(
                slot.getSlotId(), request.getReservationDate());
        java.time.LocalTime effectiveStart = exceptionOpt
                .filter(e -> e.getStartTimeOverride() != null)
                .map(TimeSlotException::getStartTimeOverride)
                .orElse(slot.getStartTime());
        java.time.LocalTime effectiveEnd = exceptionOpt
                .filter(e -> e.getEndTimeOverride() != null)
                .map(TimeSlotException::getEndTimeOverride)
                .orElse(slot.getEndTime());

        validateSlotForBooking(slot, stadium, request.getReservationDate(), effectiveStart, effectiveEnd);

        // Conflict check: 1 slot chỉ có 1 booking active tại một ngày
        if (bookingRepository.existsActiveBooking(
                stadium.getStadiumId(),
                slot.getSlotId(),
                request.getReservationDate(),
                ACTIVE_STATUSES)) {
            throw new DuplicateResourceException(
                    "Khung giờ này đã được đặt cho ngày " + request.getReservationDate()
                            + ". Vui lòng chọn khung giờ hoặc ngày khác.");
        }
        if (exceptionOpt.isPresent()) {
            TimeSlotException ex = exceptionOpt.get();
            if (Boolean.TRUE.equals(ex.getClosed())) {
                throw new BadRequestException("Khung giờ này đã tạm đóng vào ngày " + request.getReservationDate());
            }
            if (Boolean.TRUE.equals(ex.getHidden())) {
                throw new BadRequestException("Khung giờ này không tồn tại vào ngày " + request.getReservationDate());
            }
        }

        BigDecimal basePrice = (exceptionOpt.isPresent() && exceptionOpt.get().getPriceOverride() != null)
                ? exceptionOpt.get().getPriceOverride()
                : slot.getPricePerSlot();

        AccessoryComputation accessoryComp = computeAccessories(request.getAccessories());
        BigDecimal totalPrice = basePrice
                .add(accessoryComp.total)
                .add(SERVICE_FEE);

        Booking saved = persistBooking(customer, stadium, slot, request, totalPrice,
                accessoryComp.entities);

        log.info("✅ UC-CUS-01: Customer {} đặt sân {} slot {} ngày {} — bookingId={}, totalPrice={}",
                customer.getEmail(), stadium.getStadiumId(), slot.getSlotId(),
                request.getReservationDate(), saved.getBookingId(), totalPrice);

        return toBookingDetailResponse(saved, stadium, slot);
    }

    /**
     * Validate slot thuộc đúng sân, không MAINTENANCE, và chưa qua giờ.
     * Tách riêng để giữ createBooking dưới 80 dòng (checkstyle MethodLength).
     */
    private void validateSlotForBooking(TimeSlot slot, Stadium stadium, java.time.LocalDate reservationDate,
                                        java.time.LocalTime effectiveStart, java.time.LocalTime effectiveEnd) {
        if (!slot.getStadium().getStadiumId().equals(stadium.getStadiumId())) {
            throw new BadRequestException(
                    "Khung giờ #" + slot.getSlotId() + " không thuộc sân #" + stadium.getStadiumId());
        }
        if (slot.getSlotStatus() == SlotStatus.MAINTENANCE) {
            throw new BadRequestException(
                    "Khung giờ #" + slot.getSlotId() + " đang bảo trì, không thể đặt");
        }
        if (maintenanceScheduleService.isSlotUnderMaintenance(stadium, reservationDate, effectiveStart, effectiveEnd)) {
            throw new BadRequestException(
                    "Sân đang trong lịch bảo trì ngày " + reservationDate + ", không thể đặt");
        }
        LocalDateTime slotStart = LocalDateTime.of(reservationDate, effectiveStart);
        if (!slotStart.isAfter(LocalDateTime.now())) {
            throw new BadRequestException(
                    "Khung giờ đã qua — không thể đặt sân cho thời điểm trong quá khứ");
        }
    }

    /**
     * Tính tổng phụ kiện + build danh sách entity sẽ persist sau khi booking có ID.
     * Server TỰ lookup giá từ DB — KHÔNG tin unitPrice client.
     */
    private AccessoryComputation computeAccessories(List<AccessoryItem> items) {
        AccessoryComputation result = new AccessoryComputation();
        if (items == null || items.isEmpty()) {
            return result;
        }
        for (AccessoryItem item : items) {
            Accessory acc = accessoryRepository.findById(item.getAccessoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy phụ kiện với ID " + item.getAccessoryId()));

            if (!Boolean.TRUE.equals(acc.getIsAvailable())) {
                throw new BadRequestException(
                        "Phụ kiện #" + acc.getAccessoryId() + " hiện không khả dụng");
            }

            BigDecimal lineTotal = acc.getPricePerUnit()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            result.total = result.total.add(lineTotal);
            result.entities.add(BookingAccessory.builder()
                    .accessoryId(acc.getAccessoryId())
                    .quantity(item.getQuantity())
                    .unitPrice(acc.getPricePerUnit())
                    .build());
        }
        return result;
    }

    /** Persist booking + accessories trong cùng transaction. */
    private Booking persistBooking(User customer, Stadium stadium, TimeSlot slot,
                                   CreateBookingRequest request, BigDecimal totalPrice,
                                   List<BookingAccessory> accessories) {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = Booking.builder()
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(totalPrice)
                .bookingStatus(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.UNPAID)
                .reservationDate(request.getReservationDate())
                .note(request.getNote())
                .expiredAt(now.plusMinutes(PAYMENT_HOLD_MINUTES))
                .build();

        Booking saved = bookingRepository.save(booking);

        if (!accessories.isEmpty()) {
            for (BookingAccessory ba : accessories) {
                ba.setBooking(saved);
            }
            bookingAccessoryRepository.saveAll(accessories);
            log.info("🎾 UC-CUS-01: Booking #{} kèm {} phụ kiện",
                    saved.getBookingId(), accessories.size());
        }
        return saved;
    }

    /** Kết quả tính phụ kiện: tổng tiền + danh sách entity chờ persist. */
    private static final class AccessoryComputation {
        BigDecimal total = BigDecimal.ZERO;
        final List<BookingAccessory> entities = new ArrayList<>();
    }

    @Override
    @Transactional
    public BookingDetailResponse confirmPayment(UserPrincipal principal, Integer bookingId) {
        Booking booking = bookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy booking với ID " + bookingId));

        // Chỉ chủ booking mới được xác nhận.
        Integer currentUserId = principal.getUser().getUserId();
        if (booking.getUser() == null || !booking.getUser().getUserId().equals(currentUserId)) {
            throw new BadRequestException("Bạn không có quyền xác nhận thanh toán booking này");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException(
                    "Booking #" + bookingId + " đã bị huỷ (quá hạn thanh toán)");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException(
                    "Booking #" + bookingId + " không ở trạng thái chờ thanh toán. "
                            + "Hiện tại: " + booking.getBookingStatus());
        }

        // Chỉ CẢNH BÁO, không chặn: tại thời điểm này khách đã thanh toán thật qua cổng thanh toán
        // (confirmPayment không tự capture tiền — chỉ đồng bộ trạng thái nội bộ sau khi cổng đã báo
        // thành công). Chặn ở đây sẽ khiến khách mất tiền mà không có booking — tệ hơn hiện trạng.
        // createSchedule chỉ conflict-check với booking CONFIRMED (không tính PENDING_PAYMENT) nên
        // vẫn còn khe hở hẹp (~5 phút) nếu Owner đặt bảo trì đúng lúc khách đang thanh toán — log lại
        // rõ ràng để Owner/Admin chủ động xử lý thay vì âm thầm trôi qua. Check theo đúng khung giờ
        // của slot (không phải cả ngày) để tránh cảnh báo giả khi bảo trì chỉ chặn 1 khung giờ khác.
        if (booking.getStadium() != null
                && maintenanceScheduleService.isSlotUnderMaintenance(booking.getStadium(), booking.getReservationDate(),
                        booking.getSlot().getStartTime(), booking.getSlot().getEndTime())) {
            log.warn("⚠️ Booking #{} được xác nhận thanh toán trong lúc sân {} đang bảo trì ngày {} — "
                            + "cần Owner/Admin kiểm tra và liên hệ khách để xử lý (đổi lịch/hoàn tiền) nếu cần.",
                    booking.getBookingId(), booking.getStadium().getStadiumId(), booking.getReservationDate());
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setExpiredAt(null);
        Booking saved = bookingRepository.save(booking);

        log.info("💳 UC-CUS-01: Booking #{} thanh toán thành công — CONFIRMED, expiredAt cleared",
                saved.getBookingId());

        return toBookingDetailResponse(saved, saved.getStadium(), saved.getSlot());
    }

    @Override
    public BookingDetailResponse cancelBooking(UserPrincipal principal, Integer bookingId, String reason) {
        Integer currentUserId = principal.getUser().getUserId();
        
        // 1. Transaction 1: Khóa DB, cập nhật trạng thái Booking và tạo Refund Payment (PENDING)
        CancelProcessContext ctx = processLocalCancellationTx(bookingId, currentUserId, reason);

        // 2. Gọi Gateway và Transaction 2: Cập nhật trạng thái SUCCESS/FAILED
        if (ctx.refundPayment != null && ctx.originalPayment != null) {
            processGatewayRefundTx(ctx, bookingId, reason);
        }

        // 3. Fetch lại Booking mới nhất để build response
        Booking finalBooking = transactionTemplate.execute(status -> 
                bookingRepository.findDetailById(bookingId).orElse(ctx.booking));
        return toBookingDetailResponse(finalBooking, finalBooking.getStadium(), finalBooking.getSlot());
    }

    private CancelProcessContext processLocalCancellationTx(Integer bookingId, Integer currentUserId, String reason) {
        return transactionTemplate.execute(status -> {
            Booking booking = bookingRepository.findDetailById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy booking với ID " + bookingId));

            validateCancellation(booking, currentUserId);

            boolean wasReallyPaid = booking.getPaymentStatus() == PaymentStatus.PAID
                    || booking.getPaymentStatus() == PaymentStatus.DEPOSITED;
            
            booking.setBookingStatus(BookingStatus.CANCELLED);
            booking.setCancelReason(reason);
            if (wasReallyPaid) {
                booking.setPaymentStatus(PaymentStatus.REFUNDED);
            }
            booking.setExpiredAt(null);

            TimeSlot slot = booking.getSlot();
            if (slot != null && slot.getSlotStatus() == SlotStatus.BOOKED) {
                slot.setSlotStatus(SlotStatus.AVAILABLE);
                timeSlotRepository.save(slot);
            }

            Booking saved = bookingRepository.save(booking);
            Payment refundPayment = null;
            Payment originalPayment = null;

            if (wasReallyPaid) {
                originalPayment = paymentRepository.findSuccessPaymentsByBookingId(bookingId)
                        .stream().findFirst().orElse(null);
                
                if (originalPayment != null) {
                    refundPayment = Payment.builder()
                        .booking(saved)
                        .paymentMethod(originalPayment.getPaymentMethod())
                        .amount(originalPayment.getAmount().negate())
                        .transactionCode("RFND_CUST_" + originalPayment.getTransactionCode())
                        .paymentStatus(TransactionStatus.PENDING)
                        .paidAt(LocalDateTime.now())
                        .build();
                    refundPayment = paymentRepository.save(refundPayment);
                }
            }

            log.info("[UC-CUS-03] Booking #{} was cancelled by userId={}, reason={}",
                    saved.getBookingId(), currentUserId, reason);

            return new CancelProcessContext(saved, originalPayment, refundPayment);
        });
    }

    private void validateCancellation(Booking booking, Integer currentUserId) {
        boolean isCustomer = booking.getUser() != null
                && booking.getUser().getUserId().equals(currentUserId);
        boolean isVenueOwner = booking.getStadium() != null
                && booking.getStadium().getOwner() != null
                && booking.getStadium().getOwner().getUser() != null
                && booking.getStadium().getOwner().getUser().getUserId().equals(currentUserId);
        if (!isCustomer && !isVenueOwner) {
            throw new BadRequestException("Bạn không có quyền hủy đơn đặt sân này");
        }

        BookingStatus currentStatus = booking.getBookingStatus();
        if (currentStatus == BookingStatus.COMPLETED || currentStatus == BookingStatus.CANCELLED) {
            throw new BadRequestException(
                    "Không thể hủy đơn đặt sân ở trạng thái " + currentStatus);
        }
    }

    private void processGatewayRefundTx(CancelProcessContext ctx, Integer bookingId, String reason) {
        boolean gatewaySuccess = false;
        try {
            paymentService.processRefund(ctx.originalPayment, ctx.originalPayment.getAmount(), 
                    reason != null ? reason : "Khách hàng tự hủy");
            gatewaySuccess = true;
        } catch (Exception e) {
            log.error("Refund gateway failed for customer cancel booking {}", bookingId, e);
        }

        final boolean finalSuccess = gatewaySuccess;
        transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findById(ctx.refundPayment.getPaymentId()).orElse(null);
            if (payment != null) {
                payment.setPaymentStatus(finalSuccess ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);
                paymentRepository.save(payment);
            }
            return null;
        });
        
        if (!gatewaySuccess) {
            throw new BadRequestException("Lỗi hoàn tiền qua cổng thanh toán. Đơn đã được hủy nhưng tiền chưa được hoàn. Vui lòng thử lại sau.");
        }
    }

    @RequiredArgsConstructor
    private static class CancelProcessContext {
        final Booking booking;
        final Payment originalPayment;
        final Payment refundPayment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getSlotsByDate(Integer stadiumId, LocalDate date) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sân với ID " + stadiumId));

        List<TimeSlot> slots = timeSlotRepository.findByStadiumStadiumIdAndSlotStatus(
                stadium.getStadiumId(), SlotStatus.AVAILABLE);

        Set<Integer> bookedSlotIds = Set.copyOf(
                bookingRepository.findBookedSlotIds(stadiumId, date, ACTIVE_STATUSES));

        List<TimeSlotException> exceptions = timeSlotExceptionRepository.findByStadiumAndDateRange(stadiumId, date, date);
        Map<Integer, TimeSlotException> exceptionMap = exceptions.stream()
                .collect(Collectors.toMap(e -> e.getSlot().getSlotId(), e -> e));

        LocalDateTime now = LocalDateTime.now();

        java.time.LocalTime openT = stadium.getOpenTime() != null ? stadium.getOpenTime() : java.time.LocalTime.MIN;
        java.time.LocalTime closeT = stadium.getCloseTime() != null ? stadium.getCloseTime() : java.time.LocalTime.MAX;

        return slots.stream()
                .filter(slot -> {
                    TimeSlotException exception = exceptionMap.get(slot.getSlotId());
                    // Resolve effective times — apply override before comparing with open/close window.
                    java.time.LocalTime effectiveStart = (exception != null && exception.getStartTimeOverride() != null)
                            ? exception.getStartTimeOverride() : slot.getStartTime();
                    java.time.LocalTime effectiveEnd = (exception != null && exception.getEndTimeOverride() != null)
                            ? exception.getEndTimeOverride() : slot.getEndTime();
                    if (effectiveStart.isBefore(openT) || effectiveEnd.isAfter(closeT)) {
                        return false;
                    }
                    return exception == null || !Boolean.TRUE.equals(exception.getHidden());
                })
                .map(slot -> {
                    TimeSlotException exception = exceptionMap.get(slot.getSlotId());
                    java.time.LocalTime startT = (exception != null && exception.getStartTimeOverride() != null)
                            ? exception.getStartTimeOverride() : slot.getStartTime();
                    java.time.LocalTime endT = (exception != null && exception.getEndTimeOverride() != null)
                            ? exception.getEndTimeOverride() : slot.getEndTime();
                    BigDecimal price = (exception != null && exception.getPriceOverride() != null)
                            ? exception.getPriceOverride()
                            : slot.getPricePerSlot();
                    boolean isClosed = exception != null && Boolean.TRUE.equals(exception.getClosed());
                    boolean isFuture = LocalDateTime.of(date, startT).isAfter(now);
                    boolean isBooked = bookedSlotIds.contains(slot.getSlotId());

                    return TimeSlotResponse.builder()
                            .slotId(slot.getSlotId())
                            .stadiumId(slot.getStadium().getStadiumId())
                            .startTime(startT)
                            .endTime(endT)
                            .pricePerSlot(price)
                            .slotStatus(isClosed ? "OWNER_CLOSED" : (slot.getSlotStatus() != null ? slot.getSlotStatus().name() : null))
                            .available(!isBooked && isFuture && !isClosed)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklySlotResponse getWeeklySlots(Integer stadiumId, LocalDate weekStart) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sân với ID " + stadiumId));

        // Snap về thứ 2 gần nhất — cho phép FE truyền bất kỳ ngày nào trong tuần.
        LocalDate monday = snapToMonday(weekStart);
        LocalDate sunday = monday.plusDays(6);

        List<TimeSlot> slots = timeSlotRepository.findByStadiumStadiumIdAndSlotStatus(
                stadium.getStadiumId(), SlotStatus.AVAILABLE);

        List<Booking> weeklyBookings = bookingRepository.findWeeklyBookings(
                stadiumId, monday, sunday, ACTIVE_STATUSES);

        List<TimeSlotException> exceptions = timeSlotExceptionRepository.findByStadiumAndDateRange(stadiumId, monday, sunday);

        // Map (date → tập slotId đã được đặt active)
        Map<LocalDate, Set<Integer>> bookedByDate = weeklyBookings.stream()
                .collect(Collectors.groupingBy(
                        Booking::getReservationDate,
                        Collectors.mapping(
                                b -> b.getSlot().getSlotId(),
                                Collectors.toSet())));

        // Map (date → Map<slotId → exception>)
        Map<LocalDate, Map<Integer, TimeSlotException>> exceptionsByDate = exceptions.stream()
                .collect(Collectors.groupingBy(
                        TimeSlotException::getExceptionDate,
                        Collectors.toMap(
                                e -> e.getSlot().getSlotId(),
                                e -> e
                        )
                ));

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH:mm");

        // Batch — 1 query cho cả tuần thay vì gọi isSlotUnderMaintenance (1-2 query/lần) mỗi slot mỗi ngày.
        Map<LocalDate, MaintenanceScheduleService.DayMaintenance> dayMaintenanceByDate =
                maintenanceScheduleService.getDayMaintenanceForDateRange(stadium, monday, sunday);

        List<WeeklySlotDayDto> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            Set<Integer> bookedSlotIds = bookedByDate.getOrDefault(date, Set.of());
            Map<Integer, TimeSlotException> dayExceptions = exceptionsByDate.getOrDefault(date, Map.of());
            MaintenanceScheduleService.DayMaintenance dayMaintenance =
                    dayMaintenanceByDate.getOrDefault(date, MaintenanceScheduleService.DayMaintenance.NONE);
            days.add(buildWeeklySlotDay(date, stadium, slots, bookedSlotIds, dayExceptions, dayMaintenance, now, hhmm));
        }

        log.info("📅 UC-CUS-01: Stadium {} weekly slots — {}..{} ({} bookings)",
                stadiumId, monday, sunday, weeklyBookings.size());

        return WeeklySlotResponse.builder()
                .weekStart(monday.toString())
                .weekEnd(sunday.toString())
                .days(days)
                .build();
    }

    private WeeklySlotDayDto buildWeeklySlotDay(
            LocalDate date,
            Stadium stadium,
            List<TimeSlot> slots,
            Set<Integer> bookedSlotIds,
            Map<Integer, TimeSlotException> dayExceptions,
            MaintenanceScheduleService.DayMaintenance dayMaintenance,
            LocalDateTime now,
            DateTimeFormatter hhmm) {
        java.time.LocalTime openT = stadium.getOpenTime() != null ? stadium.getOpenTime() : java.time.LocalTime.MIN;
        java.time.LocalTime closeT = stadium.getCloseTime() != null ? stadium.getCloseTime() : java.time.LocalTime.MAX;

        List<WeeklySlotItemDto> daySlots = slots.stream()
                .sorted(Comparator.comparing(TimeSlot::getStartTime))
                .filter(slot -> {
                    TimeSlotException exception = dayExceptions.get(slot.getSlotId());
                    java.time.LocalTime effectiveStart = (exception != null && exception.getStartTimeOverride() != null)
                            ? exception.getStartTimeOverride() : slot.getStartTime();
                    java.time.LocalTime effectiveEnd = (exception != null && exception.getEndTimeOverride() != null)
                            ? exception.getEndTimeOverride() : slot.getEndTime();
                    if (effectiveStart.isBefore(openT) || effectiveEnd.isAfter(closeT)) {
                        return false;
                    }
                    return exception == null || !Boolean.TRUE.equals(exception.getHidden());
                })
                .map(slot -> {
                    TimeSlotException exception = dayExceptions.get(slot.getSlotId());
                    java.time.LocalTime startT = (exception != null && exception.getStartTimeOverride() != null)
                            ? exception.getStartTimeOverride() : slot.getStartTime();
                    java.time.LocalTime endT = (exception != null && exception.getEndTimeOverride() != null)
                            ? exception.getEndTimeOverride() : slot.getEndTime();
                    LocalDateTime slotStart = LocalDateTime.of(date, startT);
                    boolean isClosed = exception != null && Boolean.TRUE.equals(exception.getClosed());
                    BigDecimal price = (exception != null && exception.getPriceOverride() != null)
                            ? exception.getPriceOverride()
                            : slot.getPricePerSlot();

                    String status;
                    if (isClosed) {
                        status = "OWNER_CLOSED";
                    } else if (bookedSlotIds.contains(slot.getSlotId())) {
                        status = "BOOKED";
                    } else if (!slotStart.isAfter(now)) {
                        status = "PAST";
                    } else if (dayMaintenance.overlaps(startT, endT, date)) {
                        status = "MAINTENANCE";
                    } else {
                        status = "AVAILABLE";
                    }
                    
                    return WeeklySlotItemDto.builder()
                            .slotId(slot.getSlotId())
                            .startTime(startT != null ? startT.format(hhmm) : null)
                            .endTime(endT != null ? endT.format(hhmm) : null)
                            .price(price)
                            .status(status)
                            .build();
                })
                .toList();

        return WeeklySlotDayDto.builder()
                .date(date.toString())
                .dayName(vietnameseDayName(date))
                .slots(daySlots)
                .build();
    }

    /** Snap {@code date} về thứ 2 của tuần đó (DayOfWeek.MONDAY = 1). */
    private LocalDate snapToMonday(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1L);
    }

    /** Tên thứ tiếng Việt — dùng cho UI weekly grid. */
    private String vietnameseDayName(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "Chủ nhật";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingHistoryItemDto> getMyBookings(
            UserPrincipal principal,
            int page,
            int size,
            String statusFilter) {

        Integer userId = principal.getUser().getUserId();
        // Clamp page/size để không throw IllegalArgumentException từ PageRequest.
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        Page<Booking> bookings;
        if (statusFilter == null || statusFilter.isBlank()
                || "all".equalsIgnoreCase(statusFilter)) {
            bookings = bookingRepository
                    .findByUserUserIdOrderByReservationDateDesc(userId, pageable);
        } else {
            List<BookingStatus> statuses = mapStatusFilter(statusFilter);
            bookings = bookingRepository
                    .findByUserUserIdAndBookingStatusInOrderByReservationDateDesc(
                            userId, statuses, pageable);
        }

        log.info("📜 UC-CUS-01: Customer {} xem lịch sử đặt sân — page={}, size={}, status={}, total={}",
                principal.getUser().getEmail(), page, size, statusFilter, bookings.getTotalElements());

        return PageResponse.of(bookings.map(this::toHistoryItemDto));
    }

    /**
     * Map filter status từ FE sang danh sách {@link BookingStatus} tương ứng.
     * Không throw — filter lạ sẽ fallback về "tất cả trạng thái" để tránh
     * crash trang lịch sử.
     */
    private List<BookingStatus> mapStatusFilter(String statusFilter) {
        return switch (statusFilter.toLowerCase()) {
            case "upcoming"  -> List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.PENDING, BookingStatus.CONFIRMED);
            case "completed" -> List.of(BookingStatus.COMPLETED);
            case "cancelled" -> List.of(BookingStatus.CANCELLED);
            case "pending"   -> List.of(BookingStatus.PENDING);
            case "confirmed" -> List.of(BookingStatus.CONFIRMED);
            default -> List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED,
                    BookingStatus.COMPLETED, BookingStatus.CANCELLED);
        };
    }

    /**
     * Map {@link Booking} entity sang {@link BookingHistoryItemDto} — shape
     * phải khớp với {@code BookingHistoryItem} ở Frontend
     * ({@code frontend/src/lib/bookings-api.ts}).
     */
    private BookingHistoryItemDto toHistoryItemDto(Booking booking) {
        Stadium stadium = booking.getStadium();
        TimeSlot slot = booking.getSlot();
        SportType sportType = stadium != null ? stadium.getSportType() : null;

        String imageUrl = null;
        if (stadium != null && stadium.getImages() != null && !stadium.getImages().isEmpty()) {
            StadiumImage firstImage = stadium.getImages().iterator().next();
            imageUrl = firstImage.getImageUrl();
        }

        String dateStr = booking.getReservationDate() != null
                ? booking.getReservationDate().toString()
                : null;

        String timeStr = null;
        if (slot != null && slot.getStartTime() != null && slot.getEndTime() != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            timeStr = slot.getStartTime().format(fmt) + " - " + slot.getEndTime().format(fmt);
        }

        return BookingHistoryItemDto.builder()
                .id(String.valueOf(booking.getBookingId()))
                .displayId("BK" + String.format("%06d", booking.getBookingId()))
                .venue(stadium != null ? stadium.getStadiumName() : "Sân chưa biết")
                .sportType(sportType != null ? sportType.getSportName() : "Khác")
                .imageUrl(imageUrl != null ? imageUrl : "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=300&auto=format&fit=crop")
                .date(dateStr)
                .time(timeStr)
                .location(stadium != null ? stadium.getAddress() : null)
                .price(booking.getTotalPrice())
                .status(booking.getBookingStatus() != null
                        ? booking.getBookingStatus().name().toLowerCase()
                        : null)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC-CUS-04: Chi tiết đơn đặt sân
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(UserPrincipal principal, Integer bookingId) {
        Booking booking = bookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đặt sân với ID " + bookingId));

        Integer currentUserId = principal.getUser().getUserId();
        if (!booking.getUser().getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền xem đơn đặt sân này");
        }

        log.info("🔍 UC-CUS-04: Customer {} xem chi tiết booking #{}", principal.getUser().getEmail(), bookingId);
        return toBookingDetailResponse(booking, booking.getStadium(), booking.getSlot());
    }



    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private BookingDetailResponse toBookingDetailResponse(Booking booking, Stadium stadium, TimeSlot slot) {
        String imageUrl = null;
        if (stadium.getImages() != null && !stadium.getImages().isEmpty()) {
            imageUrl = stadium.getImages().iterator().next().getImageUrl();
        }
        String sportType = null;
        if (stadium.getSportType() != null) {
            sportType = stadium.getSportType().getSportName();
        }

        return BookingDetailResponse.builder()
                .bookingId(booking.getBookingId())
                .displayId("BK" + String.format("%06d", booking.getBookingId()))
                .reservationDate(booking.getReservationDate())
                .slot(BookingDetailResponse.SlotInfo.builder()
                        .slotId(slot.getSlotId())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .build())
                .stadium(BookingDetailResponse.StadiumInfo.builder()
                        .stadiumId(stadium.getStadiumId())
                        .stadiumName(stadium.getStadiumName())
                        .address(stadium.getAddress())
                        .sportType(sportType)
                        .imageUrl(imageUrl)
                        .build())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getBookingStatus().name().toLowerCase())
                .paymentStatus(booking.getPaymentStatus().name().toLowerCase())
                .note(booking.getNote())
                .expiredAt(booking.getExpiredAt())
                .createdAt(booking.getBookingDate())
                .build();
    }

    private TimeSlotResponse toTimeSlotResponse(TimeSlot slot, boolean bookedOnDate, boolean isFuture) {
        return TimeSlotResponse.builder()
                .slotId(slot.getSlotId())
                .stadiumId(slot.getStadium().getStadiumId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .pricePerSlot(slot.getPricePerSlot())
                .slotStatus(slot.getSlotStatus() != null ? slot.getSlotStatus().name() : null)
                .available(!bookedOnDate && isFuture)
                .build();
    }
}
