package com.sportvenue.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportvenue.dto.response.AdminDashboardResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.ComplaintStatus;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.ComplaintRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.AdminDashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final StadiumRepository stadiumRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ComplaintRepository complaintRepository;

    private static final String ROLE_CUSTOMER = "Customer";
    private static final String ROLE_OWNER = "Owner";

    /**
     * UC-ADM-01: Lấy dữ liệu tổng quan cho Admin Dashboard.
     * Cache 5 phút — trang read-heavy, không cần realtime.
     * @CacheEvict được gọi từ OwnerService, ComplaintService khi có thay đổi quan trọng.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "adminDashboard", key = "'stats'")
    public AdminDashboardResponse getDashboardData() {
        log.info("Fetching admin dashboard stats (cache miss)");

        long totalUsers = userRepository.countByRoleName(ROLE_CUSTOMER);
        long totalOwners = userRepository.countByRoleName(ROLE_OWNER);
        long totalStadiums = stadiumRepository.count();
        long totalBookings = bookingRepository.count();
        java.math.BigDecimal totalRevenue = paymentRepository.sumTotalRevenue();

        long pendingBookings = bookingRepository.countByBookingStatus(BookingStatus.PENDING)
                + bookingRepository.countByBookingStatus(BookingStatus.PENDING_PAYMENT);
        long confirmedBookings = bookingRepository.countByBookingStatus(BookingStatus.CONFIRMED);
        long cancelledBookings = bookingRepository.countByBookingStatus(BookingStatus.CANCELLED);
        long completedBookings = bookingRepository.countByBookingStatus(BookingStatus.COMPLETED);

        long pendingOwnerApprovals = ownerRepository.countByApprovedStatus(ApprovedStatus.PENDING);
        long openComplaints = complaintRepository.countByStatus(ComplaintStatus.OPEN);

        // Recent bookings
        List<Booking> recentBookingsList = bookingRepository.findTop5ByOrderByBookingDateDesc();
        List<AdminDashboardResponse.RecentBookingDto> recentBookingsDto = mapRecentBookings(recentBookingsList);

        // Booking trend — 7 ngày gần nhất cho biểu đồ (UC-ADM-01)
        List<AdminDashboardResponse.BookingTrendDto> bookingTrend = buildBookingTrend(
                LocalDate.now().minusDays(6), LocalDate.now());

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalOwners(totalOwners)
                .totalStadiums(totalStadiums)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .pendingBookings(pendingBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .completedBookings(completedBookings)
                .pendingOwnerApprovals(pendingOwnerApprovals)
                .openComplaints(openComplaints)
                .recentBookings(recentBookingsDto)
                .bookingTrend(bookingTrend)
                .build();
    }

    /**
     * UC-ADM-01: Lấy dữ liệu dashboard được lọc theo khoảng ngày.
     * Không cache vì date range thay đổi liên tục theo input người dùng.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardData(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching admin dashboard stats for date range {} → {}", startDate, endDate);

        // Các số liệu tổng quan (totalUsers, totalOwners, totalStadiums) luôn là all-time
        long totalUsers = userRepository.countByRoleName(ROLE_CUSTOMER);
        long totalOwners = userRepository.countByRoleName(ROLE_OWNER);
        long totalStadiums = stadiumRepository.count();

        // Booking stats theo date range
        long totalBookings = bookingRepository.countByDateRange(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        java.math.BigDecimal totalRevenue = paymentRepository.sumTotalRevenueByDateRange(startDateTime, endDateTime);

        long pendingBookings = bookingRepository.countByBookingStatusAndDateRange(BookingStatus.PENDING, startDate, endDate)
                + bookingRepository.countByBookingStatusAndDateRange(BookingStatus.PENDING_PAYMENT, startDate, endDate);
        long confirmedBookings = bookingRepository.countByBookingStatusAndDateRange(BookingStatus.CONFIRMED, startDate, endDate);
        long cancelledBookings = bookingRepository.countByBookingStatusAndDateRange(BookingStatus.CANCELLED, startDate, endDate);
        long completedBookings = bookingRepository.countByBookingStatusAndDateRange(BookingStatus.COMPLETED, startDate, endDate);

        // Pending approvals và open complaints vẫn là all-time
        long pendingOwnerApprovals = ownerRepository.countByApprovedStatus(ApprovedStatus.PENDING);
        long openComplaints = complaintRepository.countByStatus(ComplaintStatus.OPEN);

        // Recent bookings trong khoảng ngày
        List<Booking> recentBookingsList = bookingRepository.findTop5ByDateRangeOrderByBookingDateDesc(
                startDate, endDate, PageRequest.of(0, 5));
        List<AdminDashboardResponse.RecentBookingDto> recentBookingsDto = mapRecentBookings(recentBookingsList);

        // Booking trend theo khoảng ngày được chọn
        List<AdminDashboardResponse.BookingTrendDto> bookingTrend = buildBookingTrend(startDate, endDate);

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalOwners(totalOwners)
                .totalStadiums(totalStadiums)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .pendingBookings(pendingBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .completedBookings(completedBookings)
                .pendingOwnerApprovals(pendingOwnerApprovals)
                .openComplaints(openComplaints)
                .recentBookings(recentBookingsDto)
                .bookingTrend(bookingTrend)
                .build();
    }

    /**
     * Xoá cache dashboard — gọi khi có Owner được duyệt/từ chối,
     * hoặc có complaint mới để dashboard phản ánh dữ liệu mới nhất.
     */
    @CacheEvict(value = "adminDashboard", key = "'stats'")
    public void evictDashboardCache() {
        log.info("Admin dashboard cache evicted");
    }

    /** Map Booking entity list → RecentBookingDto list. */
    private List<AdminDashboardResponse.RecentBookingDto> mapRecentBookings(List<Booking> bookings) {
        return bookings.stream()
                .map(booking -> {
                    String customerName = booking.getUser() != null ? booking.getUser().getFullName() : "N/A";
                    String stadiumName = booking.getStadium() != null ? booking.getStadium().getStadiumName() : "N/A";
                    String timeSlot = booking.getSlot() != null
                            ? booking.getSlot().getStartTime() + " - " + booking.getSlot().getEndTime()
                            : "N/A";

                    return AdminDashboardResponse.RecentBookingDto.builder()
                            .bookingId(booking.getBookingId())
                            .customerName(customerName)
                            .stadiumName(stadiumName)
                            .totalPrice(booking.getTotalPrice())
                            .bookingStatus(booking.getBookingStatus().name())
                            .bookingDate(booking.getBookingDate())
                            .reservationDate(booking.getReservationDate())
                            .timeSlot(timeSlot)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Xây dựng danh sách booking trend theo khoảng ngày.
     * Ngày nào không có booking → trả về count = 0 (không bỏ trống).
     * Nếu khoảng > 60 ngày, gộp theo tuần để tránh quá nhiều điểm dữ liệu.
     */
    private List<AdminDashboardResponse.BookingTrendDto> buildBookingTrend(LocalDate startDate, LocalDate endDate) {
        List<Object[]> rawData = bookingRepository.countBookingsByDateRange(startDate, endDate);

        // Map date → count từ query result
        Map<LocalDate, Long> countByDate = rawData.stream()
                .collect(Collectors.toMap(
                        row -> (LocalDate) row[0],
                        row -> (Long) row[1]
                ));

        // Fill đủ mọi ngày trong khoảng, ngày không có booking = 0
        // Giới hạn tối đa 60 điểm để chart không bị overload
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        List<AdminDashboardResponse.BookingTrendDto> trend = new ArrayList<>();

        if (totalDays <= 60) {
            for (long i = 0; i < totalDays; i++) {
                LocalDate date = startDate.plusDays(i);
                trend.add(AdminDashboardResponse.BookingTrendDto.builder()
                        .date(date)
                        .count(countByDate.getOrDefault(date, 0L))
                        .build());
            }
        } else {
            // Gộp theo tuần khi khoảng ngày quá dài
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                LocalDate weekEnd = current.plusDays(6).isAfter(endDate) ? endDate : current.plusDays(6);
                final LocalDate weekStart = current;
                long weekCount = countByDate.entrySet().stream()
                        .filter(e -> !e.getKey().isBefore(weekStart) && !e.getKey().isAfter(weekEnd))
                        .mapToLong(Map.Entry::getValue)
                        .sum();
                trend.add(AdminDashboardResponse.BookingTrendDto.builder()
                        .date(weekStart)
                        .count(weekCount)
                        .build());
                current = weekEnd.plusDays(1);
            }
        }
        return trend;
    }
}
