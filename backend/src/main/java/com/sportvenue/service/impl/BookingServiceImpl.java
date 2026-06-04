package com.sportvenue.service.impl;

import com.sportvenue.dto.response.CustomerBookingResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Notification;
import com.sportvenue.entity.StadiumImage;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.NotificationRepository;
import com.sportvenue.repository.ReviewRepository;
import com.sportvenue.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(readOnly = true)
    @Override
    public List<CustomerBookingResponse> getCustomerBookings(String customerEmail) {
        log.info("Fetching bookings for customer: {}", customerEmail);
        List<Booking> bookings = bookingRepository.findByUserEmailOrderByBookingDateDesc(customerEmail);
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private CustomerBookingResponse mapToResponse(Booking b) {
        String stadiumName = b.getStadium().getStadiumName();
        String address = b.getStadium().getAddress();
        String sportTypeName = b.getStadium().getSportType() != null
                ? b.getStadium().getSportType().getSportName()
                : "Thể thao";

        // Get first image URL from stadium images
        String imageUrl = null;
        List<StadiumImage> images = b.getStadium().getImages();
        if (images != null && !images.isEmpty()) {
            imageUrl = images.get(0).getImageUrl();
        }

        // Format slot times
        String slotDate = b.getSlot().getStartTime().format(DATE_FMT);
        String startTime = b.getSlot().getStartTime().format(TIME_FMT);
        String endTime = b.getSlot().getEndTime().format(TIME_FMT);
        String time = startTime + " - " + endTime;

        // Check if booking has been reviewed
        Boolean hasReviewed = reviewRepository.existsByBookingBookingId(b.getBookingId());

        return CustomerBookingResponse.builder()
                .id(b.getBookingId())
                .bookingCode("BK-" + String.format("%06d", b.getBookingId()))
                .venueName(stadiumName)
                .venueImage(imageUrl)
                .sportType(sportTypeName)
                .location(address)
                .date(slotDate)
                .startTime(startTime)
                .endTime(endTime)
                .time(time)
                .totalPrice(b.getTotalPrice())
                .pricePerHour(b.getStadium().getPricePerHour())
                .status(b.getBookingStatus().name().toLowerCase())
                .paymentStatus(b.getPaymentStatus().name().toLowerCase())
                .paymentMethod(b.getPaymentStatus().name())
                .note(b.getNote())
                .bookingDate(b.getBookingDate().format(DATE_FMT))
                .hasReviewed(hasReviewed)
                .customerName(b.getUser().getLastName() + " " + b.getUser().getFirstName())
                .customerEmail(b.getUser().getEmail())
                .customerPhone(b.getUser().getPhoneNumber())
                .build();
    }

    @Transactional
    @Override
    public CustomerBookingResponse cancelBooking(Integer bookingId, String customerEmail) {
        log.info("Canceling booking {} for customer: {}", bookingId, customerEmail);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!booking.getUser().getEmail().equals(customerEmail)) {
            throw new RuntimeException("You don't have permission to cancel this booking");
        }

        if (!booking.getBookingStatus().name().equalsIgnoreCase("PENDING") && 
            !booking.getBookingStatus().name().equalsIgnoreCase("CONFIRMED")) {
            throw new RuntimeException("Only pending or confirmed bookings can be cancelled");
        }

        // Setting status based on enum values in database
        booking.setBookingStatus(com.sportvenue.entity.enums.BookingStatus.CANCELLED);
        
        // Also free up the time slot
        booking.getSlot().setSlotStatus(com.sportvenue.entity.enums.SlotStatus.AVAILABLE);

        bookingRepository.save(booking);
        return mapToResponse(booking);
    }

    @Override
    public List<CustomerBookingResponse> getOwnerBookings(String ownerEmail) {
        log.info("Fetching owner bookings for email: {}", ownerEmail);
        return bookingRepository.findAll().stream()
                .filter(b -> b.getStadium().getOwner() != null && 
                             b.getStadium().getOwner().getUser() != null && 
                             ownerEmail.equals(b.getStadium().getOwner().getUser().getEmail()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public CustomerBookingResponse confirmBooking(Integer bookingId, String ownerEmail) {
        log.info("Confirming booking {} by owner: {}", bookingId, ownerEmail);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!ownerEmail.equals(booking.getStadium().getOwner().getUser().getEmail())) {
            throw new RuntimeException("You don't have permission to confirm this booking");
        }

        if (!booking.getBookingStatus().name().equalsIgnoreCase("PENDING")) {
            throw new RuntimeException("Only pending bookings can be confirmed");
        }

        booking.setBookingStatus(com.sportvenue.entity.enums.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // Gửi thông báo cho khách hàng
        Notification notification = Notification.builder()
                .user(booking.getUser())
                .notificationType(NotificationType.BOOKING)
                .title("Đơn đặt sân đã được duyệt ✅")
                .message("Chủ sân đã xác nhận đơn đặt sân \"" + booking.getStadium().getStadiumName() + "\" của bạn. Hãy đến đúng giờ nhé!")
                .relatedResourceId(String.valueOf(booking.getBookingId()))
                .build();
        notificationRepository.save(notification);

        return mapToResponse(booking);
    }

    @Transactional
    @Override
    public CustomerBookingResponse rejectBooking(Integer bookingId, String ownerEmail) {
        log.info("Rejecting booking {} by owner: {}", bookingId, ownerEmail);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!ownerEmail.equals(booking.getStadium().getOwner().getUser().getEmail())) {
            throw new RuntimeException("You don't have permission to reject this booking");
        }

        if (!booking.getBookingStatus().name().equalsIgnoreCase("PENDING")) {
            throw new RuntimeException("Only pending bookings can be rejected");
        }

        booking.setBookingStatus(com.sportvenue.entity.enums.BookingStatus.CANCELLED);
        booking.getSlot().setSlotStatus(com.sportvenue.entity.enums.SlotStatus.AVAILABLE);
        
        bookingRepository.save(booking);

        // Gửi thông báo cho khách hàng
        Notification notification = Notification.builder()
                .user(booking.getUser())
                .notificationType(NotificationType.BOOKING)
                .title("Đơn đặt sân bị từ chối ❌")
                .message("Rất tiếc, chủ sân đã từ chối đơn đặt sân \"" + booking.getStadium().getStadiumName() + "\" của bạn. Bạn có thể tìm sân khác.")
                .relatedResourceId(String.valueOf(booking.getBookingId()))
                .build();
        notificationRepository.save(notification);

        return mapToResponse(booking);
    }
}
