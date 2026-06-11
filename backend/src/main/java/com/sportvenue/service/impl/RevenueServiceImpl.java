package com.sportvenue.service.impl;

import com.sportvenue.dto.response.RevenueDetailDto;
import com.sportvenue.dto.response.RevenueReportResponse;
import com.sportvenue.dto.response.OwnerDashboardSummaryResponse;
import com.sportvenue.dto.response.VenueRevenueDto;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.projection.DailyRevenueProjection;
import com.sportvenue.repository.projection.VenueRevenueProjection;
import com.sportvenue.service.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueServiceImpl implements RevenueService {

    /** Số giờ vận hành ước tính mỗi ngày — dùng để tính tỷ lệ lấp đầy. */
    private static final double OPERATIONAL_HOURS_PER_DAY = 12.0;

    /** Giới hạn tối đa khoảng thời gian query (365 ngày) để tránh OOM. */
    private static final long MAX_QUERY_DAYS = 365;

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;


    @Transactional(readOnly = true)
    @Override
    public RevenueReportResponse getRevenueReport(String ownerEmail, Integer stadiumId, LocalDateTime startDate, LocalDateTime endDate) {
        long days = validateDateRange(startDate, endDate);

        log.info("Fetching revenue report for owner: {}, stadiumId: {}, start: {}, end: {}", ownerEmail, stadiumId, startDate, endDate);

        // Lấy doanh thu theo ngày từ Payment table
        List<DailyRevenueProjection> projections = paymentRepository.getDailyRevenue(ownerEmail, stadiumId, startDate, endDate);

        List<RevenueDetailDto> details = projections.stream()
                .map(p -> RevenueDetailDto.builder()
                        .date(p.getDate().toString())
                        .revenue(p.getRevenue() != null ? p.getRevenue() : BigDecimal.ZERO)
                        .build())
                .toList(); // QUALITY-02: Java 16+ style

        BigDecimal totalRevenue = details.stream()
                .map(RevenueDetailDto::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Lấy breakdown theo sân từ cùng nguồn Payment table (BUG-02 fix)
        List<VenueRevenueProjection> venueProjections = paymentRepository.getVenueRevenueBreakdown(ownerEmail, stadiumId, startDate, endDate);

        // BUG-02 Fix: Tính totalBookings từ cùng nguồn Payment để nhất quán với totalRevenue
        Long totalBookings = venueProjections.stream()
                .mapToLong(VenueRevenueProjection::getTotalBookings)
                .sum();

        Map<Integer, BigDecimal> previousRevenueMap = getPreviousRevenueMap(ownerEmail, stadiumId, startDate, endDate, days);

        List<VenueRevenueDto> venueRevenues = buildVenueRevenueDtos(venueProjections, previousRevenueMap, days);

        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings)
                .details(details)
                .venueRevenues(venueRevenues)
                .build();
    }

    private long validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate phải trước endDate");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days > MAX_QUERY_DAYS) {
            throw new IllegalArgumentException("Khoảng thời gian không được vượt quá " + MAX_QUERY_DAYS + " ngày");
        }
        return days < 1 ? 1 : days;
    }

    private Map<Integer, BigDecimal> getPreviousRevenueMap(String ownerEmail, Integer stadiumId, LocalDateTime startDate, LocalDateTime endDate, long days) {
        LocalDateTime previousStartDate = startDate.minusDays(days);
        LocalDateTime previousEndDate = endDate.minusDays(days);
        List<VenueRevenueProjection> previousProjections = paymentRepository.getVenueRevenueBreakdown(ownerEmail, stadiumId, previousStartDate, previousEndDate);

        return previousProjections.stream()
                .collect(Collectors.toMap(
                        VenueRevenueProjection::getStadiumId,
                        p -> p.getTotalRevenue() != null ? p.getTotalRevenue() : BigDecimal.ZERO));
    }

    private List<VenueRevenueDto> buildVenueRevenueDtos(List<VenueRevenueProjection> venueProjections, Map<Integer, BigDecimal> previousRevenueMap, long days) {
        double totalAvailableHoursPerVenue = days * OPERATIONAL_HOURS_PER_DAY;

        return venueProjections.stream()
                .map(p -> {
                    BigDecimal currentRevenue = p.getTotalRevenue() != null ? p.getTotalRevenue() : BigDecimal.ZERO;

                    // BUG-04 Fix: dùng .doubleValue() tường minh tránh mất precision
                    double occupancy = (p.getTotalBookings().doubleValue() / totalAvailableHoursPerVenue) * 100.0;
                    if (occupancy > 100.0) {
                        occupancy = 100.0; // Cap tại 100%
                    }

                    // BUG-05 Fix: Trả "N/A" thay vì "+100%" khi kỳ trước = 0
                    BigDecimal prevRevenue = previousRevenueMap.getOrDefault(p.getStadiumId(), BigDecimal.ZERO);
                    String trendStr;
                    if (prevRevenue.compareTo(BigDecimal.ZERO) == 0) {
                        // Không có baseline → xu hướng không xác định
                        trendStr = currentRevenue.compareTo(BigDecimal.ZERO) > 0 ? "N/A" : "+0%";
                    } else {
                        double diff = currentRevenue.subtract(prevRevenue).doubleValue();
                        double percentage = (diff / prevRevenue.doubleValue()) * 100.0;
                        trendStr = (percentage >= 0 ? "+" : "") + Math.round(percentage) + "%";
                    }

                    return VenueRevenueDto.builder()
                            .stadiumId(p.getStadiumId())
                            .stadiumName(p.getStadiumName())
                            .totalBookings(p.getTotalBookings())
                            .totalRevenue(currentRevenue)
                            .occupancy(Math.round(occupancy * 10.0) / 10.0) // Làm tròn 1 chữ số thập phân
                            .trend(trendStr)
                            .build();
                })
                .toList(); // QUALITY-02: Java 16+ style
    }

    @Transactional(readOnly = true)
    @Override
    public OwnerDashboardSummaryResponse getDashboardSummary(String ownerEmail) {
        log.info("Calculating dashboard summary for owner: {}", ownerEmail);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = now.toLocalDate().atTime(LocalTime.MAX);

        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.toLocalDate().plusMonths(1).withDayOfMonth(1).minusDays(1).atTime(LocalTime.MAX);

        long todayBookingsCount = bookingRepository.countTodayBookingsByOwnerEmail(ownerEmail, startOfToday, endOfToday);
        BigDecimal currentMonthRevenue = paymentRepository.sumCurrentMonthRevenue(ownerEmail, startOfMonth, endOfMonth);
        long pendingBookingsCount = bookingRepository.countPendingBookingsByOwnerEmail(ownerEmail);

        // Tỷ lệ lấp đầy trong 30 ngày qua
        LocalDateTime start30DaysAgo = now.minusDays(30);
        RevenueReportResponse report = getRevenueReport(ownerEmail, null, start30DaysAgo, now);

        double averageOccupancyRate = 0.0;
        if (report.getVenueRevenues() != null && !report.getVenueRevenues().isEmpty()) {
            double totalOccupancy = report.getVenueRevenues().stream()
                    .mapToDouble(VenueRevenueDto::getOccupancy)
                    .sum();
            averageOccupancyRate = totalOccupancy / report.getVenueRevenues().size();
            averageOccupancyRate = Math.round(averageOccupancyRate * 10.0) / 10.0; // làm tròn 1 chữ số thập phân
        }

        return OwnerDashboardSummaryResponse.builder()
                .todayBookingsCount(todayBookingsCount)
                .currentMonthRevenue(currentMonthRevenue)
                .averageOccupancyRate(averageOccupancyRate)
                .pendingBookingsCount(pendingBookingsCount)
                .build();
    }
}

