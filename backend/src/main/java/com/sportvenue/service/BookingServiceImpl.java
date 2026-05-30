package com.sportvenue.service;

import com.sportvenue.dto.response.BookingResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.StadiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation cho Booking operations.
 * Xử lý logic nghiệp vụ quản lý booking cho Owner.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final OwnerRepository ownerRepository;
    private final StadiumRepository stadiumRepository;

    @Override
    public Page<BookingResponse> getBookingsByStadium(Integer ownerId, Integer stadiumId,
                                                      BookingStatus status, Pageable pageable) {
        log.info("Owner {} fetching bookings for stadium {} with status filter: {}", ownerId, stadiumId, status);

        // Verify owner exists
        Owner owner = ownerRepository.findByUserUserId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Owner profile cho userId: " + ownerId));

        // Verify stadium belongs to this owner
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân với ID: " + stadiumId));

        if (!stadium.getOwner().getOwnerId().equals(owner.getOwnerId())) {
            throw new BadRequestException("Bạn không có quyền xem booking của sân này");
        }

        Page<Booking> bookings;
        if (status != null) {
            bookings = bookingRepository.findByStadiumStadiumIdAndBookingStatusOrderByBookingDateDesc(
                    stadiumId, status, pageable);
        } else {
            bookings = bookingRepository.findByStadiumStadiumIdOrderByBookingDateDesc(stadiumId, pageable);
        }

        return bookings.map(this::toBookingResponse);
    }

    @Override
    public Page<BookingResponse> getAllBookingsByOwner(Integer ownerId, BookingStatus status,
                                                      Pageable pageable) {
        log.info("Owner {} fetching all bookings with status filter: {}", ownerId, status);

        // Verify owner exists
        Owner owner = ownerRepository.findByUserUserId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Owner profile cho userId: " + ownerId));

        Page<Booking> bookings;
        if (status != null) {
            bookings = bookingRepository.findByStadiumOwnerOwnerIdAndBookingStatusOrderByBookingDateDesc(
                    owner.getOwnerId(), status, pageable);
        } else {
            bookings = bookingRepository.findByStadiumOwnerOwnerIdOrderByBookingDateDesc(
                    owner.getOwnerId(), pageable);
        }

        return bookings.map(this::toBookingResponse);
    }

    /**
     * Convert Booking entity sang BookingResponse DTO.
     */
    private BookingResponse toBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .userId(booking.getUser().getUserId())
                .customerName(booking.getUser().getFullName())
                .customerEmail(booking.getUser().getEmail())
                .customerPhone(booking.getUser().getPhoneNumber())
                .stadiumId(booking.getStadium().getStadiumId())
                .stadiumName(booking.getStadium().getStadiumName())
                .slotId(booking.getSlot().getSlotId())
                .slotStartTime(booking.getSlot().getStartTime())
                .slotEndTime(booking.getSlot().getEndTime())
                .totalPrice(booking.getTotalPrice())
                .bookingStatus(booking.getBookingStatus())
                .paymentStatus(booking.getPaymentStatus())
                .bookingDate(booking.getBookingDate())
                .note(booking.getNote())
                .build();
    }
}
