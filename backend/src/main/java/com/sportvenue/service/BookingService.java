package com.sportvenue.service;

import com.sportvenue.dto.response.BookingResponse;
import com.sportvenue.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface cho Booking operations.
 * Cung cấp các phương thức quản lý booking cho Owner.
 */
public interface BookingService {

    /**
     * Lấy danh sách booking của một sân cụ thể — Owner dùng để quản lý.
     *
     * @param ownerId   ID của owner (lấy từ JWT)
     * @param stadiumId ID sân cần xem booking
     * @param status    filter theo trạng thái (nullable — nếu null thì lấy tất cả)
     * @param pageable  phân trang
     * @return trang booking
     */
    Page<BookingResponse> getBookingsByStadium(Integer ownerId, Integer stadiumId,
                                                BookingStatus status, Pageable pageable);

    /**
     * Lấy tất cả booking của tất cả sân thuộc owner — dùng cho overview.
     *
     * @param ownerId  ID của owner (lấy từ JWT)
     * @param status   filter theo trạng thái (nullable)
     * @param pageable phân trang
     * @return trang booking
     */
    Page<BookingResponse> getAllBookingsByOwner(Integer ownerId, BookingStatus status,
                                                Pageable pageable);
}
