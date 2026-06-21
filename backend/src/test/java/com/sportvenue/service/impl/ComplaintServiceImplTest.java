package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateComplaintRequest;
import com.sportvenue.dto.response.ComplaintResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.ComplaintPriority;
import com.sportvenue.entity.enums.ComplaintStatus;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.ComplaintRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceImplTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ComplaintServiceImpl complaintService;

    @Test
    void createComplaint_Success() {
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .bookingId(1)
                .subject("Test Subject")
                .description("Test Description")
                .build();

        User user = User.builder().userId(10).firstName("John").lastName("Doe").email("customer@example.com").build();
        User ownerUser = User.builder().userId(20).email("owner@example.com").build();
        com.sportvenue.entity.Owner owner = com.sportvenue.entity.Owner.builder().ownerId(5).user(ownerUser).build();
        com.sportvenue.entity.Stadium stadium = com.sportvenue.entity.Stadium.builder().stadiumId(100).owner(owner).stadiumName("Stadium A").build();

        Booking booking = Booking.builder()
                .bookingId(1)
                .user(user)
                .stadium(stadium)
                .bookingStatus(BookingStatus.COMPLETED)
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(complaintRepository.existsByBookingBookingId(1)).thenReturn(false);

        Complaint complaint = Complaint.builder()
                .complaintId(500)
                .booking(booking)
                .user(user)
                .subject("Test Subject")
                .content("Test Description")
                .priority(ComplaintPriority.MEDIUM)
                .status(ComplaintStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        ComplaintResponse response = complaintService.createComplaint(request, "customer@example.com");

        assertNotNull(response);
        assertEquals(500, response.getComplaintId());
        assertEquals("medium", response.getPriority());
        assertEquals("open", response.getStatus());

        verify(notificationService).createNotification(
                eq(20),
                eq("Khiếu nại mới"),
                contains("John Doe"),
                eq(NotificationType.COMPLAINT),
                eq("500")
        );
    }

    @Test
    void createComplaint_NotOwnBooking_ThrowsBadRequest() {
        CreateComplaintRequest request = CreateComplaintRequest.builder().bookingId(1).build();

        User user = User.builder().userId(10).email("customer@example.com").build();
        User anotherUser = User.builder().userId(11).email("other@example.com").build();

        Booking booking = Booking.builder()
                .bookingId(1)
                .user(anotherUser)
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () ->
                complaintService.createComplaint(request, "customer@example.com"));
    }

    @Test
    void createComplaint_BookingNotCompleted_ThrowsBadRequest() {
        CreateComplaintRequest request = CreateComplaintRequest.builder().bookingId(1).build();

        User user = User.builder().userId(10).email("customer@example.com").build();

        Booking booking = Booking.builder()
                .bookingId(1)
                .user(user)
                .bookingStatus(BookingStatus.CONFIRMED)
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () ->
                complaintService.createComplaint(request, "customer@example.com"));
    }

    @Test
    void createComplaint_DuplicateComplaint_ThrowsBadRequest() {
        CreateComplaintRequest request = CreateComplaintRequest.builder().bookingId(1).build();

        User user = User.builder().userId(10).email("customer@example.com").build();

        Booking booking = Booking.builder()
                .bookingId(1)
                .user(user)
                .bookingStatus(BookingStatus.COMPLETED)
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(complaintRepository.existsByBookingBookingId(1)).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                complaintService.createComplaint(request, "customer@example.com"));
    }
}
