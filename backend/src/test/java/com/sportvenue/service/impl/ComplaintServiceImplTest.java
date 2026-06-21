package com.sportvenue.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.ComplaintRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
    private OwnerRepository ownerRepository;

    @Mock
    private NotificationService notificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

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

    @Test
    void closeComplaint_Success() {
        User customer = User.builder().userId(10).email("customer@example.com").build();
        User ownerUser = User.builder().userId(20).email("owner@example.com").build();
        com.sportvenue.entity.Owner owner = com.sportvenue.entity.Owner.builder().ownerId(5).user(ownerUser).build();
        com.sportvenue.entity.Stadium stadium = com.sportvenue.entity.Stadium.builder().stadiumId(100).owner(owner).stadiumName("Stadium A").build();
        Booking booking = Booking.builder().bookingId(1).user(customer).stadium(stadium).build();

        Complaint complaint = Complaint.builder()
                .complaintId(500)
                .booking(booking)
                .user(customer)
                .status(ComplaintStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(complaintRepository.findById(500)).thenReturn(Optional.of(complaint));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintResponse response = complaintService.closeComplaint(500, "customer@example.com");

        assertNotNull(response);
        assertEquals("resolved", response.getStatus());
        assertEquals("Khách hàng", response.getResponses().get(0).getFrom());
        assertTrue(response.getResponses().get(0).getMessage().contains("đóng khiếu nại"));

        verify(notificationService).createNotification(
                eq(20), eq("Khiếu nại đã được đóng"), anyString(), eq(NotificationType.COMPLAINT), eq("500"));
    }

    @Test
    void closeComplaint_AlreadyResolved_ThrowsBadRequest() {
        User customer = User.builder().userId(10).email("customer@example.com").build();
        Booking booking = Booking.builder().bookingId(1).user(customer).build();

        Complaint complaint = Complaint.builder()
                .complaintId(500)
                .booking(booking)
                .user(customer)
                .status(ComplaintStatus.RESOLVED)
                .createdAt(LocalDateTime.now())
                .build();

        when(complaintRepository.findById(500)).thenReturn(Optional.of(complaint));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));

        assertThrows(BadRequestException.class, () ->
                complaintService.closeComplaint(500, "customer@example.com"));
    }

    @Test
    void closeComplaint_NotOwner_ThrowsBadRequest() {
        User customer = User.builder().userId(10).email("customer@example.com").build();
        User otherUser = User.builder().userId(99).email("other@example.com").build();
        Booking booking = Booking.builder().bookingId(1).user(otherUser).build();

        Complaint complaint = Complaint.builder()
                .complaintId(500)
                .booking(booking)
                .user(otherUser)
                .status(ComplaintStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(complaintRepository.findById(500)).thenReturn(Optional.of(complaint));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));

        assertThrows(BadRequestException.class, () ->
                complaintService.closeComplaint(500, "customer@example.com"));
    }
}
