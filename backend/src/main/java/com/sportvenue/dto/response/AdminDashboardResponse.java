package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentBookingDto {
        private Integer bookingId;
        private String customerName;
        private String stadiumName;
        private BigDecimal totalPrice;
        private String bookingStatus;
        private LocalDateTime bookingDate;
        private LocalDate reservationDate;
        private String timeSlot;
    }
}
