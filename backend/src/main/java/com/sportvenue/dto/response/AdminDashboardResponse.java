package com.sportvenue.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {
    private long totalUsers;
    private long totalOwners;
    private long totalStadiums;
    private long totalBookings;
    private BigDecimal totalRevenue;

    private long pendingBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private long completedBookings;

    private long pendingOwnerApprovals;
    private long openComplaints;

    private List<RecentBookingDto> recentBookings;

    /** Trend lượt đặt sân theo ngày — dùng cho biểu đồ 7/30 ngày trên dashboard. */
    private List<BookingTrendDto> bookingTrend;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentBookingDto {
        private Integer bookingId;
        private String customerName;
        private String stadiumName;
        private String complexName;
        private BigDecimal totalPrice;
        private String bookingStatus;
        private LocalDateTime bookingDate;
        private LocalDate reservationDate;
        private String timeSlot;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingTrendDto {
        /** Ngày theo định dạng yyyy-MM-dd */
        private LocalDate date;
        /** Số lượt đặt sân trong ngày đó */
        private long count;
    }
}
