package com.sportvenue.service.impl;

import com.sportvenue.dto.booking.CreateCustomerRecurringBookingRequest;
import com.sportvenue.dto.booking.CustomerBookingDetailDto;
import com.sportvenue.dto.booking.CustomerBookingHistoryDto;
import com.sportvenue.dto.booking.CustomerRecurringBookingResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.CustomerBookingService;
import com.sportvenue.util.StadiumUtils;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerBookingServiceImpl implements CustomerBookingService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));

    /** Trạng thái booking chiếm chỗ slot — dùng cho conflict detection (UC-CUS-01). */
    private static final List<BookingStatus> CONFLICT_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final StadiumRepository stadiumRepository;
    private final TimeSlotRepository timeSlotRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerBookingHistoryDto> getMyBookings(UserPrincipal principal, String status, int page, int size) {
        Integer userId = principal.getUser().getUserId();

        Page<Booking> pageResult;
        
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            java.util.List<BookingStatus> statuses;
            if (status.equalsIgnoreCase("upcoming")) {
                statuses = java.util.List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
            } else {
                try {
                    statuses = java.util.List.of(BookingStatus.valueOf(status.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    statuses = null; // Fallback to all or empty
                }
            }
            
            if (statuses != null) {
                pageResult = bookingRepository.findByUserUserIdAndBookingStatusInOrderByReservationDateDesc(userId, statuses, PageRequest.of(page, size));
            } else {
                pageResult = bookingRepository.findByUserUserIdOrderByReservationDateDesc(userId, PageRequest.of(page, size));
            }
        } else {
            pageResult = bookingRepository.findByUserUserIdOrderByReservationDateDesc(userId, PageRequest.of(page, size));
        }

        return PageResponse.<CustomerBookingHistoryDto>builder()
                .content(pageResult.getContent().stream().map(this::toDto).toList())
                .pageNo(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerBookingDetailDto getBookingDetail(UserPrincipal principal, Integer bookingId) {
        Integer userId = principal.getUser().getUserId();

        Booking booking = bookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân"));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xem đơn đặt sân này");
        }

        return toDetailDto(booking);
    }

    @Override
    @Transactional
    public void cancelBooking(UserPrincipal principal, Integer bookingId, String reason) {
        Integer userId = principal.getUser().getUserId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân"));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền huỷ đơn đặt sân này");
        }

        if (booking.getBookingStatus() == BookingStatus.COMPLETED || booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new com.sportvenue.exception.BadRequestException("Không thể huỷ đơn đặt sân đã hoàn thành hoặc đã huỷ");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setNote("Khách hàng huỷ: " + reason);
        
        if (booking.getSlot() != null) {
            booking.getSlot().setSlotStatus(com.sportvenue.entity.enums.SlotStatus.AVAILABLE);
            // Explicitly save the slot status change
            // (Assuming cascade or dirty checking works, but explicit save is safer if no cascade)
            // paymentRepository is already injected if needed for other logic
        }
        
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public CustomerRecurringBookingResponse createRecurringBooking(
            UserPrincipal principal,
            CreateCustomerRecurringBookingRequest request) {

        Integer userId = principal.getUser().getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        Stadium stadium = loadStadium(request.getStadiumId());
        List<TimeSlot> slots = loadAndValidateSlots(stadium, request.getSlotIds());
        List<LocalDate> playDates = expandPlayDates(
                request.getStartDate(), request.getEndDate(), request.getDaysOfWeek());
        assertNoConflicts(stadium, slots, playDates);

        String recurringGroupId = UUID.randomUUID().toString();
        List<Booking> toSave = buildBookings(user, stadium, slots, playDates, request, recurringGroupId);
        List<Booking> saved = bookingRepository.saveAll(toSave);

        log.info("✅ Tạo chuỗi đặt sân định kỳ {} — {} đơn cho user {}",
                recurringGroupId, saved.size(), userId);

        return toRecurringResponse(recurringGroupId, saved);
    }

    /** Load sân — 404 nếu không tồn tại. */
    private Stadium loadStadium(Integer stadiumId) {
        return stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));
    }

    /**
     * Load + validate tất cả slot: tồn tại, thuộc đúng sân, không trong trạng thái MAINTENANCE.
     * Trả về danh sách đã sort theo startTime (response ổn định).
     */
    private List<TimeSlot> loadAndValidateSlots(Stadium stadium, List<Integer> slotIds) {
        Set<Integer> requested = new HashSet<>(slotIds);
        List<TimeSlot> slots = timeSlotRepository.findAllById(requested);
        if (slots.size() != requested.size()) {
            throw new BadRequestException("Một hoặc nhiều khung giờ không tồn tại");
        }
        for (TimeSlot slot : slots) {
            if (!slot.getStadium().getStadiumId().equals(stadium.getStadiumId())) {
                throw new BadRequestException(
                        "Khung giờ #" + slot.getSlotId() + " không thuộc sân này");
            }
            if (slot.getSlotStatus() == SlotStatus.MAINTENANCE) {
                throw new BadRequestException(
                        "Khung giờ #" + slot.getSlotId() + " đang bảo trì, không thể đặt");
            }
        }
        slots.sort(Comparator.comparing(TimeSlot::getStartTime));
        return slots;
    }

    /** Mở rộng khoảng ngày theo các thứ trong tuần — 400 nếu không khớp ngày nào. */
    private List<LocalDate> expandPlayDates(
            LocalDate start, LocalDate end, List<DayOfWeek> daysOfWeek) {
        Set<DayOfWeek> daySet = new HashSet<>(daysOfWeek);
        List<LocalDate> playDates = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (daySet.contains(d.getDayOfWeek())) {
                playDates.add(d);
            }
        }
        if (playDates.isEmpty()) {
            throw new BadRequestException(
                    "Không có ngày nào trong khoảng đã chọn khớp với các thứ đã chọn");
        }
        return playDates;
    }

    /**
     * Kiểm tra conflict per-(stadium, slot, date). Nếu phát hiện bất kỳ conflict nào,
     * ném BadRequestException liệt kê chi tiết — toàn bộ request bị rollback (all-or-nothing).
     */
    private void assertNoConflicts(
            Stadium stadium, List<TimeSlot> slots, List<LocalDate> playDates) {
        List<CustomerRecurringBookingResponse.SkippedDateItem> conflicts = new ArrayList<>();
        for (LocalDate date : playDates) {
            for (TimeSlot slot : slots) {
                if (bookingRepository.existsByStadiumSlotAndDate(
                        stadium.getStadiumId(), slot.getSlotId(), date, CONFLICT_STATUSES)) {
                    conflicts.add(buildConflictItem(date, slot));
                }
            }
        }
        if (!conflicts.isEmpty()) {
            String detail = conflicts.stream()
                    .map(c -> formatConflict(c))
                    .collect(Collectors.joining("; "));
            throw new BadRequestException(
                    "Có " + conflicts.size() + " khung giờ đã bị đặt: " + detail);
        }
    }

    private CustomerRecurringBookingResponse.SkippedDateItem buildConflictItem(
            LocalDate date, TimeSlot slot) {
        return CustomerRecurringBookingResponse.SkippedDateItem.builder()
                .playDate(date)
                .slotId(slot.getSlotId())
                .slotStart(slot.getStartTime())
                .slotEnd(slot.getEndTime())
                .reason("Khung giờ đã được đặt bởi khách hàng khác")
                .build();
    }

    private String formatConflict(CustomerRecurringBookingResponse.SkippedDateItem c) {
        String start = c.getSlotStart() != null ? c.getSlotStart().format(TIME_FMT) : "?";
        String end = c.getSlotEnd() != null ? c.getSlotEnd().format(TIME_FMT) : "?";
        return String.format("%s %s-%s (slot %d)",
                c.getPlayDate().format(DATE_FMT), start, end, c.getSlotId());
    }

    /** Build toàn bộ Booking rows (chưa save) cho cartesian product (date × slot). */
    private List<Booking> buildBookings(
            User user, Stadium stadium, List<TimeSlot> slots, List<LocalDate> playDates,
            CreateCustomerRecurringBookingRequest request, String recurringGroupId) {
        List<Booking> toSave = new ArrayList<>();
        for (LocalDate date : playDates) {
            for (TimeSlot slot : slots) {
                toSave.add(Booking.builder()
                        .user(user)
                        .stadium(stadium)
                        .slot(slot)
                        .totalPrice(slot.getPricePerSlot())
                        .bookingStatus(BookingStatus.PENDING)
                        .paymentStatus(PaymentStatus.UNPAID)
                        .reservationDate(date)
                        .recurringGroupId(recurringGroupId)
                        .note(request.getNote())
                        .build());
            }
        }
        return toSave;
    }

    /** Map saved Booking rows → response DTO (sort theo ngày + giờ cho UX dễ đọc). */
    private CustomerRecurringBookingResponse toRecurringResponse(
            String recurringGroupId, List<Booking> saved) {
        List<CustomerRecurringBookingResponse.CreatedBookingItem> created = saved.stream()
                .sorted(Comparator
                        .comparing(Booking::getReservationDate)
                        .thenComparing(b -> b.getSlot().getStartTime()))
                .map(b -> CustomerRecurringBookingResponse.CreatedBookingItem.builder()
                        .id(b.getBookingId())
                        .playDate(b.getReservationDate())
                        .slotStart(b.getSlot().getStartTime())
                        .slotEnd(b.getSlot().getEndTime())
                        .totalPrice(b.getTotalPrice())
                        .build())
                .toList();
        return CustomerRecurringBookingResponse.builder()
                .recurringGroupId(recurringGroupId)
                .totalCreated(created.size())
                .totalSkipped(0)
                .createdBookings(created)
                .skippedDates(List.of())
                .build();
    }

    private CustomerBookingHistoryDto toDto(Booking booking) {
        String date = booking.getReservationDate().format(DATE_FMT);
        String time = booking.getSlot().getStartTime().format(TIME_FMT)
                + " - "
                + booking.getSlot().getEndTime().format(TIME_FMT);

        return new CustomerBookingHistoryDto(
                String.valueOf(booking.getBookingId()),
                "BK" + String.format("%06d", booking.getBookingId()),
                booking.getStadium().getStadiumName(),
                toSportLabel(booking.getStadium().getSportType().getSportName()),
                resolveImageUrl(booking.getStadium()),
                date,
                time,
                booking.getStadium().getAddress(),
                booking.getTotalPrice(),
                toFrontendStatus(booking.getBookingStatus()),
                booking.getRecurringGroupId()
        );
    }

    private CustomerBookingDetailDto toDetailDto(Booking booking) {
        return CustomerBookingDetailDto.builder()
                .id(String.valueOf(booking.getBookingId()))
                .displayId("BK" + String.format("%06d", booking.getBookingId()))
                .venueName(booking.getStadium().getStadiumName())
                .sportType(toSportLabel(booking.getStadium().getSportType().getSportName()))
                .imageUrl(resolveImageUrl(booking.getStadium()))
                .playDate(booking.getReservationDate().format(DATE_FMT))
                .startTime(booking.getSlot().getStartTime().format(TIME_FMT))
                .endTime(booking.getSlot().getEndTime().format(TIME_FMT))
                .address(booking.getStadium().getAddress())
                .totalPrice(booking.getTotalPrice())
                .status(toFrontendStatus(booking.getBookingStatus()))
                .paymentStatus(booking.getPaymentStatus().name().toLowerCase())
                .createdAt(booking.getBookingDate().format(DATETIME_FMT))
                .note(booking.getNote())
                .build();
    }

    private String toFrontendStatus(BookingStatus status) {
        return switch (status) {
            case PENDING -> "pending";
            case CONFIRMED -> "confirmed";
            case COMPLETED -> "completed";
            case CANCELLED -> "cancelled";
        };
    }

    private String resolveImageUrl(Stadium stadium) {
        return StadiumUtils.resolveImageUrl(stadium);
    }

    private String toSportLabel(String sportName) {
        return StadiumUtils.toSportLabel(sportName);
    }
}
