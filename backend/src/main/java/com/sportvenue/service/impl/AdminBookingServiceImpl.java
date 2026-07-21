package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminBookingListResponse;
import com.sportvenue.dto.response.AdminBookingResponse;
import com.sportvenue.dto.response.AdminBookingStatsResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.entity.enums.WalletTransactionType;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.PaymentRepository;
import com.sportvenue.repository.WalletTransactionRepository;
import com.sportvenue.repository.specification.BookingSpecification;
import com.sportvenue.service.AdminBookingService;
import com.sportvenue.util.StadiumUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.sportvenue.constant.DateConstants;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBookingServiceImpl implements AdminBookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminBookingListResponse getAdminBookings(
            String search,
            BookingStatus bookingStatus,
            PaymentStatus paymentStatus,
            LocalDate startDate,
            LocalDate endDate,
            Integer stadiumId,
            Integer ownerId,
            Pageable pageable) {

        log.info("Admin querying bookings: search={}, bookingStatus={}, paymentStatus={}, startDate={}, endDate={}, stadiumId={}, ownerId={}",
                search, bookingStatus, paymentStatus, startDate, endDate, stadiumId, ownerId);

        // 1. Build Specification and Query paginated list
        Specification<Booking> spec = BookingSpecification.withDynamicFilter(
                search, bookingStatus, paymentStatus, startDate, endDate, stadiumId, ownerId);

        Page<Booking> page = bookingRepository.findAll(spec, pageable);

        // Map to response list
        List<AdminBookingResponse> content = page.getContent().stream()
                .map(this::mapToAdminBookingResponse)
                .collect(Collectors.toList());

        PageResponse<AdminBookingResponse> paginatedBookings = PageResponse.of(page, content);

        // 2. Calculate aggregate stats — pass ALL active filters so stats always match the displayed list.
        AdminBookingStatsResponse stats = calculateStats(
                startDate, endDate, bookingStatus, paymentStatus, stadiumId, ownerId, page.getTotalElements());

        return AdminBookingListResponse.builder()
                .bookings(paginatedBookings)
                .stats(stats)
                .build();
    }

    private AdminBookingResponse mapToAdminBookingResponse(Booking booking) {
        User customer = booking.getUser();
        Stadium stadium = booking.getStadium();
        TimeSlot slot = booking.getSlot();

        String customerName = "N/A";
        String customerEmail = "N/A";
        if (customer != null) {
            customerName = (customer.getLastName() != null ? customer.getLastName().trim() : "")
                    + " " + (customer.getFirstName() != null ? customer.getFirstName().trim() : "");
            customerName = customerName.trim().isEmpty() ? "N/A" : customerName;
            customerEmail = customer.getEmail();
        }

        String stadiumName = stadium != null ? stadium.getStadiumName() : "N/A";
        String complexName = stadium != null ? StadiumUtils.resolveComplexName(stadium) : null;
        String ownerName = "N/A";
        if (stadium != null) {
            Owner owner = stadium.resolveOwner();
            if (owner != null) {
                if (owner.getBusinessName() != null && !owner.getBusinessName().trim().isEmpty()) {
                    ownerName = owner.getBusinessName().trim();
                } else if (owner.getUser() != null) {
                    ownerName = (owner.getUser().getLastName() != null ? owner.getUser().getLastName().trim() : "")
                            + " " + (owner.getUser().getFirstName() != null ? owner.getUser().getFirstName().trim() : "");
                    ownerName = ownerName.trim().isEmpty() ? "N/A" : ownerName;
                }
            }
        }

        String timeSlotStr = slot != null
                ? slot.getStartTime() + " - " + slot.getEndTime()
                : "N/A";

        return AdminBookingResponse.builder()
                .bookingId(booking.getBookingId())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .stadiumName(stadiumName)
                .complexName(complexName)
                .ownerName(ownerName)
                .totalPrice(booking.getTotalPrice())
                .serviceFee(booking.getServiceFee())
                .bookingStatus(booking.getBookingStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .bookingDate(booking.getBookingDate())
                .reservationDate(booking.getReservationDate())
                .timeSlot(timeSlotStr)
                .note(booking.getNote())
                .cancelReason(booking.getCancelReason())
                .build();
    }

    /**
     * Tính Gross/Refund/Fee/Net từ nguồn sự thật duy nhất:
     * - Gross  = Payment (amount > 0, SUCCESS) — tiền thực thu
     * - Refund = Payment (amount < 0, SUCCESS) — tiền đã hoàn, giá trị dương
     * - Fee    = Platform Wallet (SERVICE_FEE_CREDIT) — phí Platform thực thu
     * - Net    = Gross - Refund - Fee
     *
     * Tất cả filter (bookingStatus, paymentStatus, stadiumId, ownerId, startDate, endDate) đều
     * được áp dụng cho cả aggregate lẫn đếm booking — đảm bảo stats LUÔN khớp với danh sách
     * hiển thị trên cùng tập filter, tránh regression khi Admin lọc theo status / sân / owner.
     *
     * Cột ngày lọc dùng thống nhất là b.reservationDate (ngày chơi thực tế), khớp với
     * Dashboard / Báo cáo doanh thu (RevenueServiceImpl) và danh sách Owner Bookings.
     */
    private AdminBookingStatsResponse calculateStats(
            LocalDate startDate, LocalDate endDate,
            BookingStatus bookingStatus, PaymentStatus paymentStatus,
            Integer stadiumId, Integer ownerId,
            long totalBookings) {

        // BookingStatus list — null = all statuses
        List<BookingStatus> bookingStatuses = bookingStatus != null
                ? List.of(bookingStatus)
                : Arrays.asList(BookingStatus.values());

        // PaymentStatus list — null = all statuses
        List<PaymentStatus> paymentStatuses = paymentStatus != null
                ? List.of(paymentStatus)
                : Arrays.asList(PaymentStatus.values());

        LocalDate effectiveStart = startDate != null ? startDate : DateConstants.EPOCH_START;
        LocalDate effectiveEnd = endDate != null ? endDate : DateConstants.EPOCH_END;

        // Gross: Payment amount > 0, SUCCESS, lọc theo b.reservationDate
        BigDecimal totalGMV = paymentRepository.sumPlatformGrossByFilters(
                effectiveStart, effectiveEnd, bookingStatuses, paymentStatuses, stadiumId, ownerId);
        if (totalGMV == null) {
            totalGMV = BigDecimal.ZERO;
        }

        // Refund: Payment amount < 0, SUCCESS, lọc theo b.reservationDate
        BigDecimal totalRefund = paymentRepository.sumPlatformRefundByFilters(
                effectiveStart, effectiveEnd, bookingStatuses, paymentStatuses, stadiumId, ownerId);
        if (totalRefund == null) {
            totalRefund = BigDecimal.ZERO;
        }

        // Fee: Platform Wallet SERVICE_FEE_CREDIT, lọc theo b.reservationDate
        BigDecimal totalServiceFee = walletTransactionRepository.sumPlatformFeeByFilters(
                WalletTransactionType.SERVICE_FEE_CREDIT,
                effectiveStart, effectiveEnd, bookingStatuses, paymentStatuses, stadiumId, ownerId);
        if (totalServiceFee == null) {
            totalServiceFee = BigDecimal.ZERO;
        }

        BigDecimal totalNet = totalGMV.subtract(totalRefund).subtract(totalServiceFee);

        log.info("Admin stats [{} → {}] bookingStatus={}, paymentStatus={}, stadiumId={}, ownerId={} "
                        + "— Gross={}, Refund={}, Fee={}, Net={}, Bookings={}",
                startDate, endDate, bookingStatus, paymentStatus, stadiumId, ownerId,
                totalGMV, totalRefund, totalServiceFee, totalNet, totalBookings);

        return AdminBookingStatsResponse.builder()
                .totalBookings(totalBookings)
                .totalGMV(totalGMV)
                .totalRefund(totalRefund)
                .totalServiceFee(totalServiceFee)
                .totalNet(totalNet)
                .build();
    }
}
