package com.sportvenue.service;

import com.sportvenue.dto.response.AdminBookingListResponse;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AdminBookingService {
    AdminBookingListResponse getAdminBookings(
            String search,
            BookingStatus bookingStatus,
            PaymentStatus paymentStatus,
            LocalDate startDate,
            LocalDate endDate,
            Integer stadiumId,
            Integer ownerId,
            Pageable pageable);
}
