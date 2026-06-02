package com.sportvenue.service.impl;

import com.sportvenue.dto.response.RevenueDetailDto;
import com.sportvenue.dto.response.RevenueReportResponse;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.projection.DailyRevenueProjection;
import com.sportvenue.service.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings != null ? totalBookings : 0L)
                .details(details)
                .build();
    }
}
