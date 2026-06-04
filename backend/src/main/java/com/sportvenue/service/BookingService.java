package com.sportvenue.service;

import com.sportvenue.dto.response.CustomerBookingResponse;

import java.util.List;

public interface BookingService {

    /** Lấy toàn bộ lịch sử đặt sân của một Customer. */
    List<CustomerBookingResponse> getCustomerBookings(String customerEmail);

    /** Hủy đặt sân. */
    CustomerBookingResponse cancelBooking(Integer bookingId, String customerEmail);

    /** Owner lấy danh sách đơn đặt sân. */
    List<CustomerBookingResponse> getOwnerBookings(String ownerEmail);

    /** Owner xác nhận đơn đặt sân. */
    CustomerBookingResponse confirmBooking(Integer bookingId, String ownerEmail);

    /** Owner từ chối đơn đặt sân. */
    CustomerBookingResponse rejectBooking(Integer bookingId, String ownerEmail);
}
