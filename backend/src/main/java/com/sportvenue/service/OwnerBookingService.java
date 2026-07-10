package com.sportvenue.service;

import com.sportvenue.dto.request.BookingActionRequest;
import com.sportvenue.dto.response.BookingResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.util.StadiumUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.sportvenue.dto.response.WeeklySlotResponse;


/**
 * Service xử lý nghiệp vụ quản lý đặt sân cho Owner.
 * Bao gồm: xem danh sách, xác nhận, từ chối booking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerBookingService {

    private final BookingRepository bookingRepository;
    private final OwnerRepository ownerRepository;
    private final StadiumRepository stadiumRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingService bookingService;

    @Transactional(readOnly = true)
    public WeeklySlotResponse getOwnerWeeklySlots(Integer userId, Integer stadiumId, LocalDate weekStart) {
        Owner owner = findOwnerByUserId(userId);
        validateStadiumOwnership(stadiumId, owner.getOwnerId());
        WeeklySlotResponse response = bookingService.getWeeklySlots(stadiumId, weekStart);
        LocalDate monday = LocalDate.parse(response.getWeekStart());
        List<Booking> bookings = bookingRepository.findWeeklyBookings(stadiumId, monday, monday.plusDays(6),
                List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.PENDING, BookingStatus.CONFIRMED));
        Map<String, Booking> byDateAndSlot = bookings.stream().collect(Collectors.toMap(
                booking -> booking.getReservationDate() + ":" + booking.getSlot().getSlotId(),
                Function.identity(), (first, ignored) -> first));
        response.getDays().forEach(day -> day.getSlots().forEach(slot -> {
            Booking booking = byDateAndSlot.get(day.getDate() + ":" + slot.getSlotId());
            if (booking != null) {
                slot.setBookingId(booking.getBookingId());
                slot.setCustomerId(booking.getUser().getUserId());
                slot.setCustomerDisplayName(abbreviateCustomerName(booking.getUser()));
            }
        }));
        return response;
    }

    private String abbreviateCustomerName(User user) {
        if (user == null) {
            return "Khách hàng";
        }
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        if (firstName == null || firstName.isBlank()) {
            return "Khách hàng";
        }
        if (lastName == null || lastName.isBlank()) {
            return firstName.trim();
        }
        String familyName = lastName.trim().split("\\s+")[0];
        return firstName.trim() + " " + Character.toUpperCase(familyName.charAt(0)) + ".";
    }

    /**
     * UC-OWN-06: Lấy danh sách booking của tất cả sân thuộc Owner.
     *
     * @param userId ID của user đăng nhập (phải có role Owner)
     * @param stadiumId (optional) filter theo sân cụ thể
     * @param status (optional) filter theo trạng thái booking
     * @param pageable phân trang
     * @return trang booking responses
     */
    @Transactional(readOnly = true)
    public Page<BookingResponse> getOwnerBookings(
            Integer userId, Integer stadiumId,
            BookingStatus status, Pageable pageable) {

        Owner owner = findOwnerByUserId(userId);

        // Nếu filter theo sân cụ thể — kiểm tra quyền sở hữu
        if (stadiumId != null) {
            validateStadiumOwnership(stadiumId, owner.getOwnerId());

            if (status != null) {
                return bookingRepository
                        .findByStadiumStadiumIdAndBookingStatus(stadiumId, status, pageable)
                        .map(this::toBookingResponse);
            }
            return bookingRepository
                    .findByStadiumStadiumIdOrderByBookingDateDesc(stadiumId, pageable)
                    .map(this::toBookingResponse);
        }

        // Lấy tất cả sân của owner rồi query booking (đã fix N+1)
        return bookingRepository
                .findByOwnerIdAndStatus(owner.getOwnerId(), status, pageable)
                .map(this::toBookingResponse);
    }

    /**
     * UC-OWN-07: Xác nhận hoặc từ chối đơn đặt sân.
     *
     * @param userId ID của Owner
     * @param bookingId ID của booking cần xử lý
     * @param request action (CONFIRM/REJECT) và lý do
     * @return booking response sau khi cập nhật
     */
    @Transactional
    public BookingResponse processBooking(
            Integer userId, Integer bookingId,
            BookingActionRequest request) {

        Owner owner = findOwnerByUserId(userId);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đặt sân: " + bookingId));

        // Kiểm tra quyền sở hữu sân
        validateStadiumOwnership(
                booking.getStadium().getStadiumId(),
                owner.getOwnerId());

        // Chỉ xử lý booking đang Pending
        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new BadRequestException(
                    "Chỉ có thể xác nhận/từ chối đơn đang ở trạng thái Chờ xác nhận. "
                    + "Trạng thái hiện tại: " + booking.getBookingStatus());
        }

        if (request.getAction() == BookingActionRequest.Action.CONFIRM) {
            return confirmBooking(booking);
        } else {
            return rejectBooking(booking, request.getReason());
        }
    }

    private BookingResponse confirmBooking(Booking booking) {
        TimeSlot slot = booking.getSlot();
        if (slot.getSlotStatus() != SlotStatus.AVAILABLE) {
            throw new BadRequestException("Khung giờ này đã được đặt bởi một khách hàng khác.");
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);

        // Cập nhật slot thành Booked (Tự động flush nhờ Dirty Checking của JPA)
        slot.setSlotStatus(SlotStatus.BOOKED);

        log.info("✅ Booking #{} đã được xác nhận", booking.getBookingId());
        return toBookingResponse(booking);
    }

    private BookingResponse rejectBooking(Booking booking, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException(
                    "Vui lòng nhập lý do khi từ chối đơn đặt sân.");
        }

        // Design decision: Sử dụng CANCELLED thay vì tạo status riêng (REJECTED) cho Owner từ chối.
        // PO quyết định gộp chung để đơn giản hóa flow, lý do từ chối sẽ được ghi vào note.
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setNote("Lý do từ chối: " + reason);

        // Chỉ giải phóng slot cho người khác đặt khi không có đơn CONFIRMED khác đang chiếm giữ slot này
        TimeSlot slot = booking.getSlot();
        if (slot.getSlotStatus() != SlotStatus.BOOKED) {
            slot.setSlotStatus(SlotStatus.AVAILABLE);
        }

        log.info("❌ Booking #{} đã bị từ chối. Lý do: {}",
                booking.getBookingId(), reason);
        return toBookingResponse(booking);
    }

    // ── Helper methods ────────────────────────────────────────────

    private Owner findOwnerByUserId(Integer userId) {
        return ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ chủ sân cho user: " + userId));
    }

    private void validateStadiumOwnership(
            Integer stadiumId, Integer ownerId) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sân: " + stadiumId));
        Owner resolvedOwner = stadium.resolveOwner();
        if (resolvedOwner == null || !resolvedOwner.getOwnerId().equals(ownerId)) {
            throw new BadRequestException(
                    "Bạn không có quyền quản lý sân này.");
        }
    }

    private BookingResponse toBookingResponse(Booking booking) {
        User customer = booking.getUser();
        Stadium stadium = booking.getStadium();
        TimeSlot slot = booking.getSlot();

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .customer(BookingResponse.CustomerInfo.builder()
                        .userId(customer.getUserId())
                        .fullName(customer.getLastName()
                                + " " + customer.getFirstName())
                        .email(maskEmail(customer.getEmail()))
                        .phoneNumber(maskPhone(customer.getPhoneNumber()))
                        .avatarUrl(customer.getAvatarUrl())
                        .build())
                .stadium(BookingResponse.StadiumInfo.builder()
                        .stadiumId(stadium.getStadiumId())
                        .stadiumName(stadium.getStadiumName())
                        .address(StadiumUtils.resolveAddress(stadium))
                        .sportType(stadium.getSportType().getSportName())
                        .build())
                .slot(BookingResponse.SlotInfo.builder()
                        .slotId(slot.getSlotId())
                        .startTime(LocalDateTime.of(booking.getReservationDate(), slot.getStartTime()))
                        .endTime(LocalDateTime.of(booking.getReservationDate(), slot.getEndTime()))
                        .build())
                .totalPrice(booking.getTotalPrice())
                .bookingStatus(booking.getBookingStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .note(booking.getNote())
                .bookingDate(booking.getBookingDate())
                .recurringGroupId(booking.getRecurringGroupId())
                .build();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts.length < 2) {
            return email;
        }
        String name = parts[0];
        if (name.length() > 3) {
            name = name.substring(0, 3) + "***";
        } else {
            name = "***";
        }
        return name + "@" + parts[1];
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
