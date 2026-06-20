package com.sportvenue.service.impl;

import com.sportvenue.dto.booking.BookingDetailResponse;
import com.sportvenue.dto.booking.BookingHistoryItemDto;
import com.sportvenue.dto.request.CreateBookingRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.Booking;
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
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

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
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final UserRepository userRepository;
    private final StadiumRepository stadiumRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public BookingDetailResponse createBooking(UserPrincipal principal, CreateBookingRequest request) {
        User customer = userRepository.findById(principal.getUser().getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Stadium stadium = stadiumRepository.findById(request.getStadiumId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sân với ID " + request.getStadiumId()));

        TimeSlot slot = timeSlotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy khung giờ với ID " + request.getSlotId()));

        // Slot phải thuộc đúng sân
        if (!slot.getStadium().getStadiumId().equals(stadium.getStadiumId())) {
            throw new BadRequestException(
                    "Khung giờ #" + slot.getSlotId() + " không thuộc sân #" + stadium.getStadiumId());
        }

        // Không đặt slot MAINTENANCE
        if (slot.getSlotStatus() == SlotStatus.MAINTENANCE) {
            throw new BadRequestException(
                    "Khung giờ #" + slot.getSlotId() + " đang bảo trì, không thể đặt");
        }

        LocalDateTime slotStart = LocalDateTime.of(request.getReservationDate(), slot.getStartTime());
        if (!slotStart.isAfter(LocalDateTime.now())) {
            throw new BadRequestException(
                    "Khung giờ đã qua — không thể đặt sân cho thời điểm trong quá khứ");
        }

        // Conflict check: 1 slot chỉ có 1 booking active tại một ngày
        if (bookingRepository.existsByStadium_StadiumIdAndSlot_SlotIdAndReservationDateAndBookingStatusIn(
                stadium.getStadiumId(),
                slot.getSlotId(),
                request.getReservationDate(),
                ACTIVE_STATUSES)) {
            throw new DuplicateResourceException(
                    "Khung giờ này đã được đặt cho ngày " + request.getReservationDate()
                            + ". Vui lòng chọn khung giờ hoặc ngày khác.");
        }

        Booking booking = Booking.builder()
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(slot.getPricePerSlot())
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .reservationDate(request.getReservationDate())
                .note(request.getNote())
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("✅ UC-CUS-01: Customer {} đặt sân {} slot {} ngày {} — bookingId={}",
                customer.getEmail(), stadium.getStadiumId(), slot.getSlotId(),
                request.getReservationDate(), saved.getBookingId());

        return toBookingDetailResponse(saved, stadium, slot);
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

        LocalDateTime now = LocalDateTime.now();

        return slots.stream()
                .map(slot -> toTimeSlotResponse(slot, bookedSlotIds.contains(slot.getSlotId()),
                        LocalDateTime.of(date, slot.getStartTime()).isAfter(now)))
                .toList();
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
            case "upcoming"  -> List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
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
                .imageUrl(imageUrl != null ? imageUrl : "/images/stadium1.jpg")
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
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private BookingDetailResponse toBookingDetailResponse(Booking booking, Stadium stadium, TimeSlot slot) {
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
                        .build())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getBookingStatus().name().toLowerCase())
                .paymentStatus(booking.getPaymentStatus().name().toLowerCase())
                .note(booking.getNote())
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
