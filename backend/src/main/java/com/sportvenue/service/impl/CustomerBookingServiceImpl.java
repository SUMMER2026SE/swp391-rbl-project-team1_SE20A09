package com.sportvenue.service.impl;

import com.sportvenue.dto.booking.CustomerBookingHistoryDto;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.CustomerBookingService;
import com.sportvenue.util.StadiumUtils;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerBookingServiceImpl implements CustomerBookingService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("vi-VN"));

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final com.sportvenue.repository.PaymentRepository paymentRepository;

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
                pageResult = bookingRepository.findByUserUserIdAndBookingStatusInOrderByBookingDateDesc(userId, statuses, PageRequest.of(page, size));
            } else {
                pageResult = bookingRepository.findByUserUserIdOrderByBookingDateDesc(userId, PageRequest.of(page, size));
            }
        } else {
            pageResult = bookingRepository.findByUserUserIdOrderByBookingDateDesc(userId, PageRequest.of(page, size));
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
    @Transactional
    public void cancelBooking(UserPrincipal principal, Integer bookingId, String reason) {
        Integer userId = principal.getUser().getUserId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân"));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new com.sportvenue.exception.BadRequestException("Bạn không có quyền huỷ đơn đặt sân này");
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

    private CustomerBookingHistoryDto toDto(Booking booking) {
        String date = booking.getBookingDate().format(DATE_FMT);
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
                toFrontendStatus(booking.getBookingStatus())
        );
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
