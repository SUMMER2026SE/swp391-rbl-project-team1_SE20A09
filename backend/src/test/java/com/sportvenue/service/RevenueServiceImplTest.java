package com.sportvenue.service;

import com.sportvenue.dto.response.RevenueReportResponse;
import com.sportvenue.dto.response.VenueRevenueDto;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.projection.DailyRevenueProjection;
import com.sportvenue.repository.projection.VenueRevenueProjection;
import com.sportvenue.service.impl.RevenueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class RevenueServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    private RevenueServiceImpl revenueService;

    @BeforeEach
    void setUp() {
        revenueService = new RevenueServiceImpl(paymentRepository, bookingRepository, stadiumRepository);
    }

    @Test
    void getRevenueReport_Success() {
        String ownerEmail = "owner@sportvenue.com";
        LocalDateTime startDate = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 6, 3, 0, 0);

        // Daily projections
        DailyRevenueProjection d1 = Mockito.mock(DailyRevenueProjection.class);
        when(d1.getDate()).thenReturn(LocalDate.of(2026, 6, 1));
        when(d1.getRevenue()).thenReturn(BigDecimal.valueOf(500000));

        DailyRevenueProjection d2 = Mockito.mock(DailyRevenueProjection.class);
        when(d2.getDate()).thenReturn(LocalDate.of(2026, 6, 2));
        when(d2.getRevenue()).thenReturn(BigDecimal.valueOf(1000000));

        when(paymentRepository.getDailyRevenue(ownerEmail, null, startDate, endDate))
                .thenReturn(List.of(d1, d2));

        // Venue breakdown projections
        VenueRevenueProjection v1 = Mockito.mock(VenueRevenueProjection.class);
        when(v1.getStadiumId()).thenReturn(1);
        when(v1.getStadiumName()).thenReturn("Stadium A");
        when(v1.getTotalBookings()).thenReturn(10L);
        when(v1.getTotalRevenue()).thenReturn(BigDecimal.valueOf(1500000));

        when(paymentRepository.getVenueRevenueBreakdown(ownerEmail, null, startDate, endDate))
                .thenReturn(List.of(v1));

        // Previous period projections (2 days earlier: May 30 to May 31)
        LocalDateTime prevStart = startDate.minusDays(2);
        LocalDateTime prevEnd = endDate.minusDays(2);
        VenueRevenueProjection prevV1 = Mockito.mock(VenueRevenueProjection.class);
        when(prevV1.getStadiumId()).thenReturn(1);
        when(prevV1.getTotalRevenue()).thenReturn(BigDecimal.valueOf(1000000));

        when(paymentRepository.getVenueRevenueBreakdown(ownerEmail, null, prevStart, prevEnd))
                .thenReturn(List.of(prevV1));

        // Execute
        RevenueReportResponse report = revenueService.getRevenueReport(ownerEmail, null, startDate, endDate);

        // Verify
        assertNotNull(report);
        assertEquals(BigDecimal.valueOf(1500000), report.getTotalRevenue());
        assertEquals(10L, report.getTotalBookings());
        assertEquals(2, report.getDetails().size());
        assertEquals(1, report.getVenueRevenues().size());

        VenueRevenueDto venueRevenue = report.getVenueRevenues().getFirst();
        assertEquals(1, venueRevenue.getStadiumId());
        assertEquals("Stadium A", venueRevenue.getStadiumName());
        assertEquals(10L, venueRevenue.getTotalBookings());
        assertEquals(BigDecimal.valueOf(1500000), venueRevenue.getTotalRevenue());

        // Occupancy check: (10 bookings / (2 days * 12 hours)) * 100% = 41.666...% -> round to 41.7%
        assertEquals(41.7, venueRevenue.getOccupancy());

        // Trend check: ((1,500,000 - 1,000,000) / 1,000,000) * 100% = +50%
        assertEquals("+50%", venueRevenue.getTrend());
    }

    @Test
    void getRevenueReport_StartDateAfterEndDate_ThrowsException() {
        String ownerEmail = "owner@sportvenue.com";
        LocalDateTime startDate = LocalDateTime.of(2026, 6, 2, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 6, 1, 0, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                revenueService.getRevenueReport(ownerEmail, null, startDate, endDate)
        );
        assertEquals("startDate phải trước endDate", ex.getMessage());
    }

    @Test
    void getRevenueReport_Exceeds365Days_ThrowsException() {
        String ownerEmail = "owner@sportvenue.com";
        LocalDateTime startDate = LocalDateTime.of(2025, 6, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 6, 2, 0, 0); // 366 days

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                revenueService.getRevenueReport(ownerEmail, null, startDate, endDate)
        );
        assertEquals("Khoảng thời gian không được vượt quá 365 ngày", ex.getMessage());
    }

    @Test
    void getRevenueReport_TrendNA_WhenPreviousRevenueIsZero() {
        String ownerEmail = "owner@sportvenue.com";
        LocalDateTime startDate = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 6, 3, 0, 0);

        // Daily projections
        DailyRevenueProjection d1 = Mockito.mock(DailyRevenueProjection.class);
        when(d1.getDate()).thenReturn(LocalDate.of(2026, 6, 1));
        when(d1.getRevenue()).thenReturn(BigDecimal.valueOf(500000));
        when(paymentRepository.getDailyRevenue(ownerEmail, null, startDate, endDate))
                .thenReturn(List.of(d1));

        // Venue breakdown projections
        VenueRevenueProjection v1 = Mockito.mock(VenueRevenueProjection.class);
        when(v1.getStadiumId()).thenReturn(1);
        when(v1.getStadiumName()).thenReturn("Stadium A");
        when(v1.getTotalBookings()).thenReturn(5L);
        when(v1.getTotalRevenue()).thenReturn(BigDecimal.valueOf(500000));
        when(paymentRepository.getVenueRevenueBreakdown(ownerEmail, null, startDate, endDate))
                .thenReturn(List.of(v1));

        // Previous period projections return zero revenue or empty
        LocalDateTime prevStart = startDate.minusDays(2);
        LocalDateTime prevEnd = endDate.minusDays(2);
        when(paymentRepository.getVenueRevenueBreakdown(ownerEmail, null, prevStart, prevEnd))
                .thenReturn(List.of()); // Returns no projections, mapping defaults previous revenue to zero

        // Execute
        RevenueReportResponse report = revenueService.getRevenueReport(ownerEmail, null, startDate, endDate);

        // Verify trend is N/A when previous period revenue is 0
        VenueRevenueDto venueRevenue = report.getVenueRevenues().getFirst();
        assertEquals("N/A", venueRevenue.getTrend());
    }

    @Test
    void getDashboardSummary_Success() {
        String ownerEmail = "owner@sportvenue.com";
        LocalDateTime now = LocalDateTime.now();

        // Mock today bookings count
        when(bookingRepository.countTodayBookingsByOwnerEmail(eq(ownerEmail), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);

        // Mock current month revenue
        when(paymentRepository.sumCurrentMonthRevenue(eq(ownerEmail), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(10000000));

        // Mock pending bookings count
        when(bookingRepository.countPendingBookingsByOwnerEmail(ownerEmail))
                .thenReturn(3L);

        // Mock bookings in 30 days
        when(bookingRepository.countBookingsByOwnerAndDateRange(eq(ownerEmail), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L);

        // Mock stadiums count
        when(stadiumRepository.countStadiumsByOwnerEmail(ownerEmail))
                .thenReturn(1L);

        // Execute
        com.sportvenue.dto.response.OwnerDashboardSummaryResponse summary = revenueService.getDashboardSummary(ownerEmail);

        // Verify
        assertNotNull(summary);
        assertEquals(5L, summary.getTodayBookingsCount());
        assertEquals(BigDecimal.valueOf(10000000), summary.getCurrentMonthRevenue());
        assertEquals(3L, summary.getPendingBookingsCount());
        
        // occupancy for 30 days, 1 stadium and 10 bookings is (10 / (30 * 12 * 1)) * 100% = 2.77% -> round to 2.8%
        assertEquals(2.8, summary.getAverageOccupancyRate());
    }
}

