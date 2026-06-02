package com.sportvenue.service.impl;

import com.sportvenue.dto.response.RevenueDetailDto;
import com.sportvenue.dto.response.RevenueReportResponse;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.projection.DailyRevenueProjection;
import com.sportvenue.repository.projection.VenueRevenueProjection;
import com.sportvenue.dto.response.VenueRevenueDto;
import com.sportvenue.service.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueServiceImpl implements RevenueService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    @Override
    public RevenueReportResponse getRevenueReport(String ownerEmail, Integer stadiumId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching revenue report for owner: {}, stadiumId: {}, start: {}, end: {}", ownerEmail, stadiumId, startDate, endDate);

        List<DailyRevenueProjection> projections = paymentRepository.getDailyRevenue(ownerEmail, stadiumId, startDate, endDate);

        List<RevenueDetailDto> details = projections.stream()
                .map(p -> RevenueDetailDto.builder()
                        .date(p.getDate().toString())
                        .revenue(p.getRevenue() != null ? p.getRevenue() : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalRevenue = details.stream()
                .map(RevenueDetailDto::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalBookings = bookingRepository.countBookingsForRevenue(ownerEmail, stadiumId, startDate, endDate);

        // 1. Calculate Occupancy Rate (Estimate: 12 hours open per day per venue, 1 booking = 1 hour roughly)
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days < 1) {
            days = 1; // Minimum 1 day
        }
        double totalAvailableHoursPerVenue = days * 12.0;

        // 2. Fetch current period venue revenues
        List<VenueRevenueProjection> venueProjections = paymentRepository.getVenueRevenueBreakdown(ownerEmail, stadiumId, startDate, endDate);
        
        // 3. Fetch previous period for Trend calculation
        LocalDateTime previousStartDate = startDate.minusDays(days);
        LocalDateTime previousEndDate = endDate.minusDays(days);
        List<VenueRevenueProjection> previousProjections = paymentRepository.getVenueRevenueBreakdown(ownerEmail, stadiumId, previousStartDate, previousEndDate);
        
        Map<Integer, BigDecimal> previousRevenueMap = previousProjections.stream()
                .collect(Collectors.toMap(VenueRevenueProjection::getStadiumId, p -> p.getTotalRevenue() != null ? p.getTotalRevenue() : BigDecimal.ZERO));

        List<VenueRevenueDto> venueRevenues = venueProjections.stream()
                .map(p -> {
                    BigDecimal currentRevenue = p.getTotalRevenue() != null ? p.getTotalRevenue() : BigDecimal.ZERO;
                    
                    // Occupancy
                    double occupancy = (p.getTotalBookings() / totalAvailableHoursPerVenue) * 100.0;
                    if (occupancy > 100.0) {
                        occupancy = 100.0; // Cap at 100%
                    }
                    
                    // Trend
                    BigDecimal prevRevenue = previousRevenueMap.getOrDefault(p.getStadiumId(), BigDecimal.ZERO);
                    String trendStr = "+0%";
                    if (prevRevenue.compareTo(BigDecimal.ZERO) == 0) {
                        if (currentRevenue.compareTo(BigDecimal.ZERO) > 0) {
                            trendStr = "+100%";
                        }
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
                            .occupancy(Math.round(occupancy * 10.0) / 10.0) // 1 decimal place
                            .trend(trendStr)
                            .build();
                })
                .collect(Collectors.toList());

        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings != null ? totalBookings : 0L)
                .details(details)
                .venueRevenues(venueRevenues)
                .build();
    }
}
