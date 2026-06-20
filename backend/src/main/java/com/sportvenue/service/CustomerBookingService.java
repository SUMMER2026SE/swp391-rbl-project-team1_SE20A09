package com.sportvenue.service;

import com.sportvenue.dto.booking.CreateCustomerRecurringBookingRequest;
import com.sportvenue.dto.booking.CustomerBookingDetailDto;
import com.sportvenue.dto.booking.CustomerBookingHistoryDto;
import com.sportvenue.dto.booking.CustomerRecurringBookingResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.security.UserPrincipal;

public interface CustomerBookingService {
    PageResponse<CustomerBookingHistoryDto> getMyBookings(UserPrincipal principal, String status, int page, int size);

    CustomerBookingDetailDto getBookingDetail(UserPrincipal principal, Integer bookingId);

    void cancelBooking(UserPrincipal principal, Integer bookingId, String reason);

    /**
     * UC-CUS-01: Tạo chuỗi đặt sân định kỳ.
     * Mỗi (date, slot) hợp lệ sẽ tạo một Booking row, liên kết bởi recurringGroupId (UUID).
     * Nếu có bất kỳ (date, slot) nào conflict → throw BadRequestException, toàn bộ rollback (all-or-nothing).
     */
    CustomerRecurringBookingResponse createRecurringBooking(
            UserPrincipal principal,
            CreateCustomerRecurringBookingRequest request);
}
