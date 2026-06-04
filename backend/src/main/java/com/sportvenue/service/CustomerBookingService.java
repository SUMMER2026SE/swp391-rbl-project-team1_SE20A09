package com.sportvenue.service;

import com.sportvenue.dto.booking.CustomerBookingHistoryDto;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumImage;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.UserPrincipal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerBookingService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("vi-VN"));

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<CustomerBookingHistoryDto> getMyBookings(UserPrincipal principal) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        Pageable page = PageRequest.of(0, 100);
        Page<Booking> pageResult =
                bookingRepository.findByUserUserIdOrderByBookingDateDesc(user.getUserId(), page);

        return pageResult.getContent().stream().map(this::toDto).toList();
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
        if (stadium.getImages() == null || stadium.getImages().isEmpty()) {
            return "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800";
        }
        return stadium.getImages().stream()
                .sorted(Comparator.comparing(StadiumImage::getUploadedAt))
                .map(StadiumImage::getImageUrl)
                .findFirst()
                .orElse("https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800");
    }

    private String toSportLabel(String sportName) {
        return switch (sportName) {
            case "Football" -> "Bóng đá";
            case "Badminton" -> "Cầu lông";
            case "Basketball" -> "Bóng rổ";
            case "Tennis" -> "Quần vợt";
            case "Pickleball" -> "Pickleball";
            default -> sportName;
        };
    }
}
