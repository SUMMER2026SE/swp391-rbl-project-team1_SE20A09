package com.sportvenue.service.impl;

import com.sportvenue.dto.booking.CustomerBookingHistoryDto;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.Booking;
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerBookingHistoryDto> getMyBookings(UserPrincipal principal, int page, int size) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        Page<Booking> pageResult = bookingRepository
                .findByUserUserIdOrderByBookingDateDesc(user.getUserId(), PageRequest.of(page, size));

        return PageResponse.<CustomerBookingHistoryDto>builder()
                .content(pageResult.getContent().stream().map(this::toDto).toList())
                .pageNo(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    private CustomerBookingHistoryDto toDto(Booking booking) {
        String date = booking.getSlot().getStartTime().format(DATE_FMT);
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
