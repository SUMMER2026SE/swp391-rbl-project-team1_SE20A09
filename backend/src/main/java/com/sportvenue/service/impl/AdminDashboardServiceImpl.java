package com.sportvenue.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardData() {
        log.info("Fetching admin dashboard stats");

        long totalUsers = userRepository.countByRoleName(ROLE_CUSTOMER);
        long totalOwners = userRepository.countByRoleName(ROLE_OWNER);
        long totalStadiums = stadiumRepository.count();
        long totalBookings = bookingRepository.count();
        java.math.BigDecimal totalRevenue = paymentRepository.sumTotalRevenue();

        long pendingBookings = bookingRepository.countByBookingStatus(BookingStatus.PENDING);
        long confirmedBookings = bookingRepository.countByBookingStatus(BookingStatus.CONFIRMED);
        long cancelledBookings = bookingRepository.countByBookingStatus(BookingStatus.CANCELLED);
        long completedBookings = bookingRepository.countByBookingStatus(BookingStatus.COMPLETED);

        long pendingOwnerApprovals = ownerRepository.countByApprovedStatus(ApprovedStatus.PENDING);
        long openComplaints = complaintRepository.countByStatus(ComplaintStatus.OPEN);

        List<Booking> recentBookingsList = bookingRepository.findTop5ByOrderByBookingDateDesc();
        List<AdminDashboardResponse.RecentBookingDto> recentBookingsDto = recentBookingsList.stream()
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
                .build();
    }
}
