package com.sportvenue.service;

import com.sportvenue.dto.home.HomeDashboardResponse;
import com.sportvenue.dto.home.PersonalStatsDto;
import com.sportvenue.dto.home.UpcomingBookingDto;
import com.sportvenue.dto.home.VenueSummaryDto;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumImage;
import com.sportvenue.entity.User;
import com.sportvenue.entity.UserFavoriteStadium;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.ReviewRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserFavoriteStadiumRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("vi-VN"));

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final UserFavoriteStadiumRepository favoriteRepository;
    private final StadiumRepository stadiumRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public HomeDashboardResponse getDashboard(UserPrincipal principal) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        Integer userId = user.getUserId();
        LocalDateTime now = LocalDateTime.now();
        Pageable upcomingPage = PageRequest.of(0, 10);

        List<Booking> upcoming = bookingRepository.findUpcomingByUserId(userId, now, upcomingPage);
        List<UserFavoriteStadium> favorites = favoriteRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        List<Integer> favoriteIds = favorites.stream()
                .map(f -> f.getStadium().getStadiumId())
                .toList();

        List<Stadium> recommended = loadRecommended(favoriteIds);
        List<Booking> completed = bookingRepository.findCompletedByUserId(userId);

        return new HomeDashboardResponse(
                (int) bookingRepository.countByUserUserId(userId),
                favorites.size(),
                user.getUserPoint() != null ? user.getUserPoint() : 0,
                upcoming.stream().map(this::toUpcomingDto).toList(),
                favorites.stream().map(f -> toVenueDto(f.getStadium(), true)).toList(),
                recommended.stream().map(s -> toVenueDto(s, false)).toList(),
                List.of(),
                buildPersonalStats(completed)
        );
    }

    private List<Stadium> loadRecommended(List<Integer> favoriteIds) {
        Pageable page = PageRequest.of(0, 6);
        if (favoriteIds.isEmpty()) {
            return stadiumRepository
                    .findByStadiumStatus(StadiumStatus.AVAILABLE, page)
                    .getContent();
        }
        return stadiumRepository.findRecommendedExcluding(favoriteIds, page);
    }

    private PersonalStatsDto buildPersonalStats(List<Booking> completed) {
        if (completed.isEmpty()) {
            return new PersonalStatsDto(0, 0, "—");
        }

        long totalMinutes = 0;
        Map<String, Long> sportCounts = new HashMap<>();

        for (Booking booking : completed) {
            if (booking.getSlot() != null && booking.getSlot().getStartTime() != null
                    && booking.getSlot().getEndTime() != null) {
                totalMinutes += Duration.between(
                        booking.getSlot().getStartTime(),
                        booking.getSlot().getEndTime()).toMinutes();
            }
            if (booking.getStadium() != null && booking.getStadium().getSportType() != null) {
                String sport = booking.getStadium().getSportType().getSportName();
                sportCounts.merge(sport, 1L, Long::sum);
            }
        }

        int totalHours = (int) Math.round(totalMinutes / 60.0);
        long venuesVisited = completed.stream()
                .map(b -> b.getStadium().getStadiumId())
                .distinct()
                .count();

        String favoriteSport = sportCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .map(this::toSportLabel)
                .orElse("—");

        return new PersonalStatsDto(totalHours, venuesVisited, favoriteSport);
    }

    private UpcomingBookingDto toUpcomingDto(Booking booking) {
        String date = booking.getSlot().getStartTime().format(DATE_FMT);
        String time = booking.getSlot().getStartTime().format(TIME_FMT)
                + " – "
                + booking.getSlot().getEndTime().format(TIME_FMT);
        String status = booking.getBookingStatus() == BookingStatus.CONFIRMED
                ? "confirmed"
                : "pending";

        return new UpcomingBookingDto(
                String.valueOf(booking.getBookingId()),
                booking.getStadium().getStadiumName(),
                toSportLabel(booking.getStadium().getSportType().getSportName()),
                booking.getStadium().getAddress(),
                date,
                time,
                status,
                resolveImageUrl(booking.getStadium())
        );
    }

    private VenueSummaryDto toVenueDto(Stadium stadium, boolean saved) {
        BigDecimal rating = stadium.getAverageRating() != null
                ? stadium.getAverageRating().setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(5.0);

        return new VenueSummaryDto(
                stadium.getStadiumId(),
                stadium.getStadiumName(),
                toSportLabel(stadium.getSportType().getSportName()),
                toSportKey(stadium.getSportType().getSportName()),
                stadium.getPricePerHour(),
                rating,
                reviewRepository.countByStadiumStadiumId(stadium.getStadiumId()),
                stadium.getAddress(),
                resolveImageUrl(stadium),
                saved
        );
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

    private String toSportKey(String sportName) {
        return switch (sportName) {
            case "Football" -> "football";
            case "Badminton" -> "badminton";
            case "Basketball" -> "basketball";
            case "Tennis" -> "tennis";
            case "Pickleball" -> "pickleball";
            default -> sportName.toLowerCase(Locale.ROOT);
        };
    }
}
