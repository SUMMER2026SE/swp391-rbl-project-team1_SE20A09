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
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.specification.BookingSpecification;
import com.sportvenue.service.AdminBookingService;
import com.sportvenue.util.StadiumUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBookingServiceImpl implements AdminBookingService {

    private final BookingRepository bookingRepository;

    @PersistenceContext
    private final EntityManager entityManager;

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

        // 2. Fetch aggregate stats using optimized Criteria API under the same filters
        AdminBookingStatsResponse stats = calculateStats(spec);

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

    private AdminBookingStatsResponse calculateStats(Specification<Booking> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Booking> root = query.from(Booking.class);

        // Recreate the filters from Specification
        Predicate filterPredicate = spec.toPredicate(root, query, cb);

        // GMV: PENDING, CONFIRMED, COMPLETED
        Expression<BigDecimal> gmvExpression = cb.coalesce(
            cb.sum(
                cb.<BigDecimal>selectCase()
                    .when(root.get("bookingStatus").in(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED), root.get("totalPrice"))
                    .otherwise(BigDecimal.ZERO)
            ),
            BigDecimal.ZERO
        );

        // Service Fee: COMPLETED
        Expression<BigDecimal> serviceFeeExpression = cb.coalesce(
            cb.sum(
                cb.<BigDecimal>selectCase()
                    .when(cb.equal(root.get("bookingStatus"), BookingStatus.COMPLETED), root.get("serviceFee"))
                    .otherwise(BigDecimal.ZERO)
            ),
            BigDecimal.ZERO
        );

        query.multiselect(
            cb.count(root),
            gmvExpression,
            serviceFeeExpression
        );

        if (filterPredicate != null) {
            query.where(filterPredicate);
        }

        try {
            Object[] result = entityManager.createQuery(query).getSingleResult();
            long totalBookings = result[0] != null ? (Long) result[0] : 0L;
            BigDecimal totalGMV = result[1] != null ? (BigDecimal) result[1] : BigDecimal.ZERO;
            BigDecimal totalServiceFee = result[2] != null ? (BigDecimal) result[2] : BigDecimal.ZERO;

            return AdminBookingStatsResponse.builder()
                    .totalBookings(totalBookings)
                    .totalGMV(totalGMV)
                    .totalServiceFee(totalServiceFee)
                    .build();
        } catch (Exception e) {
            log.error("Error calculating admin booking stats", e);
            return AdminBookingStatsResponse.builder()
                    .totalBookings(0)
                    .totalGMV(BigDecimal.ZERO)
                    .totalServiceFee(BigDecimal.ZERO)
                    .build();
        }
    }
}
