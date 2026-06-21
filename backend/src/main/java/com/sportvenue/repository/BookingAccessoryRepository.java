package com.sportvenue.repository;

import com.sportvenue.entity.BookingAccessory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * UC-CUS-01: Repository cho {@link BookingAccessory} — phụ kiện kèm theo booking.
 */
@Repository
public interface BookingAccessoryRepository extends JpaRepository<BookingAccessory, Integer> {

    /** Lấy tất cả phụ kiện của một booking — dùng để render chi tiết booking. */
    List<BookingAccessory> findByBookingBookingId(Integer bookingId);
}
