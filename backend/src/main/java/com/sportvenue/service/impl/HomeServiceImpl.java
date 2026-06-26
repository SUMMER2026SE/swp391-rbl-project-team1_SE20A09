package com.sportvenue.service.impl;

import com.sportvenue.dto.home.HomeDashboardResponse;
import com.sportvenue.dto.home.PersonalStatsDto;
import com.sportvenue.dto.home.UpcomingBookingDto;
import com.sportvenue.dto.home.VenueSummaryDto;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.ReviewRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.HomeService;
import com.sportvenue.util.StadiumUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("vi-VN"));

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final StadiumRepository stadiumRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public HomeDashboardResponse getDashboard(UserPrincipal principal) {
        if (principal == null) {
            log.error("UserPrincipal is null");
            throw new ResourceNotFoundException("Người dùng chưa đăng nhập");
        }
        
        log.info("Loading dashboard for user: {}", principal.getUsername());
        
        try {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found in database: {}", principal.getUsername());
                    return new ResourceNotFoundException("Người dùng không tồn tại: " + principal.getUsername());
                });

        Integer userId = user.getUserId();
        LocalDateTime now = LocalDateTime.now();
        Pageable upcomingPage = PageRequest.of(0, 10);

        List<Booking> upcoming = bookingRepository.findUpcomingByUserId(userId, now, upcomingPage);
        log.info("Found {} upcoming bookings for user {}", upcoming.size(), userId);

        // Lấy danh sách sân đã từng đặt (real-time từ booking history, trừ CANCELLED)
        Pageable bookedPage = PageRequest.of(0, 6);
        List<Integer> bookedStadiumIds = bookingRepository.findDistinctBookedStadiumIds(userId, bookedPage);
        List<Stadium> bookedStadiums = bookedStadiumIds.isEmpty()
                ? List.of()
                : stadiumRepository.findAllById(bookedStadiumIds);

        // Sắp xếp lại theo thứ tự từ query (most recent booking first)
        Map<Integer, Integer> orderMap = new java.util.HashMap<>();
        for (int i = 0; i < bookedStadiumIds.size(); i++) {
            orderMap.put(bookedStadiumIds.get(i), i);
        }
        bookedStadiums = bookedStadiums.stream()
                .sorted(java.util.Comparator.comparingInt(s -> orderMap.getOrDefault(s.getStadiumId(), 999)))
                .toList();

        List<Stadium> recommended = loadRecommended(bookedStadiumIds);

        // Batch load review counts — tránh N+1
        List<Stadium> allDisplayedStadiums = new java.util.ArrayList<>(bookedStadiums);
        allDisplayedStadiums.addAll(recommended);

        List<Integer> allIds = allDisplayedStadiums.stream()
                .map(Stadium::getStadiumId)
                .distinct()
                .toList();

        Map<Integer, Long> reviewCountMap;
        if (allIds.isEmpty()) {
            reviewCountMap = Map.of();
        } else {
            reviewCountMap = reviewRepository.countReviewsByStadiumIdIn(allIds)
                    .stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));
        }

        // Thống kê từ DB — không load entity thô
        long venuesVisited = bookingRepository.countDistinctCompletedVenues(userId);
        PersonalStatsDto stats = buildPersonalStatsFromDb(userId, venuesVisited);

        return new HomeDashboardResponse(
                (int) bookingRepository.countByUserUserId(userId),
                bookedStadiums.size(),
                user.getUserPoint() != null ? user.getUserPoint() : 0,
                upcoming.stream().map(this::toUpcomingDto).toList(),
                bookedStadiums.stream().map(s -> toVenueDto(s, true, reviewCountMap)).toList(),
                recommended.stream().map(s -> toVenueDto(s, false, reviewCountMap)).toList(),
                List.of(),
                stats
        );
        } catch (Exception e) {
            log.error("Error loading dashboard for user {}: {}", principal.getUsername(), e.getMessage(), e);
            throw e;
        }
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

    private PersonalStatsDto buildPersonalStatsFromDb(Integer userId, long venuesVisited) {
        long totalMinutes = bookingRepository.sumCompletedPlayMinutes(userId);
        int totalHours = (int) Math.round(totalMinutes / 60.0);

        List<Object[]> topSport = bookingRepository.findTopSportByUserId(userId, PageRequest.of(0, 1));
        String favoriteSport = topSport.isEmpty()
                ? "—"
                : toSportLabel((String) topSport.get(0)[0]);

        return new PersonalStatsDto(totalHours, venuesVisited, favoriteSport);
    }

    private UpcomingBookingDto toUpcomingDto(Booking booking) {
        // Guard null slot — phòng tránh NPE nếu dữ liệu DB bị thiếu slot
        if (booking.getSlot() == null) {
            log.warn("Booking {} has no slot, skipping date/time", booking.getBookingId());
            return new UpcomingBookingDto(
                    String.valueOf(booking.getBookingId()),
                    booking.getStadium() != null ? booking.getStadium().getStadiumName() : "N/A",
                    "N/A", "N/A", "N/A", "N/A", "pending", null
            );
        }

        String date = booking.getSlot().getStartTime().format(DATE_FMT);
        String time = booking.getSlot().getStartTime().format(TIME_FMT)
                + " – "
                + booking.getSlot().getEndTime().format(TIME_FMT);
        String status = booking.getBookingStatus() == BookingStatus.CONFIRMED
                ? "confirmed"
                : "pending";

        String sportName = (booking.getStadium() != null
                && booking.getStadium().getSportType() != null)
                ? booking.getStadium().getSportType().getSportName()
                : "N/A";

        return new UpcomingBookingDto(
                String.valueOf(booking.getBookingId()),
                booking.getStadium() != null ? booking.getStadium().getStadiumName() : "N/A",
                toSportLabel(sportName),
                booking.getStadium() != null ? booking.getStadium().getAddress() : "N/A",
                date,
                time,
                status,
                booking.getStadium() != null ? StadiumUtils.resolveImageUrl(booking.getStadium()) : null
        );
    }

    private VenueSummaryDto toVenueDto(Stadium stadium, boolean saved, Map<Integer, Long> reviewCountMap) {
        BigDecimal rating = stadium.getAverageRating() != null
                ? stadium.getAverageRating().setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(5.0);

        long reviewCount = reviewCountMap.getOrDefault(stadium.getStadiumId(), 0L);

        String sportName = stadium.getSportType() != null
                ? stadium.getSportType().getSportName()
                : "N/A";

        return new VenueSummaryDto(
                stadium.getStadiumId(),
                stadium.getStadiumName(),
                toSportLabel(sportName),
                toSportKey(sportName),
                stadium.getPricePerHour(),
                rating,
                reviewCount,
                stadium.getAddress(),
                StadiumUtils.resolveImageUrl(stadium),
                saved
        );
    }

    private String toSportLabel(String sportName) {
        return StadiumUtils.toSportLabel(sportName);
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
