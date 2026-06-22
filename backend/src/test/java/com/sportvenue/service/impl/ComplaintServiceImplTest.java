package com.sportvenue.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvenue.dto.request.CreateComplaintRequest;
import com.sportvenue.dto.request.ReplyComplaintRequest;
import com.sportvenue.dto.request.ResolveComplaintRequest;
import com.sportvenue.dto.response.ComplaintResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.Role;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private SimpMessagingTemplate messagingTemplate;

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
        when(complaintRepository.existsByBookingBookingIdAndStatusNot(1, ComplaintStatus.RESOLVED)).thenReturn(false);

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
        when(complaintRepository.existsByBookingBookingIdAndStatusNot(1, ComplaintStatus.RESOLVED)).thenReturn(true);

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

    @Test
    void getAllComplaints_Success() {
        List<Complaint> mockList = new ArrayList<>();
        User user = User.builder().userId(10).email("customer@example.com").build();
        User ownerUser = User.builder().userId(20).email("owner@example.com").build();
        com.sportvenue.entity.Owner owner = com.sportvenue.entity.Owner.builder().ownerId(5).user(ownerUser).build();
        com.sportvenue.entity.Stadium stadium = com.sportvenue.entity.Stadium.builder().stadiumId(100).owner(owner).stadiumName("Stadium A").build();
        Booking booking = Booking.builder().bookingId(1).user(user).stadium(stadium).build();

        mockList.add(Complaint.builder()
                .complaintId(1)
                .booking(booking)
                .user(user)
                .status(ComplaintStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build());

        Pageable pageable = PageRequest.of(0, 20);
        when(complaintRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(mockList));

        Page<ComplaintResponse> result = complaintService.getAllComplaints(pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().get(0).getComplaintId());
    }

    @Test
    void resolveComplaintByAdmin_Success() {
        ResolveComplaintRequest request = ResolveComplaintRequest.builder().resolution("Phạt chủ sân").build();

        User user = User.builder().userId(10).email("customer@example.com").build();
        User ownerUser = User.builder().userId(20).email("owner@example.com").build();
        com.sportvenue.entity.Owner owner = com.sportvenue.entity.Owner.builder().ownerId(5).user(ownerUser).build();
        com.sportvenue.entity.Stadium stadium = com.sportvenue.entity.Stadium.builder().stadiumId(100).owner(owner).stadiumName("Stadium A").build();
        Booking booking = Booking.builder().bookingId(1).user(user).stadium(stadium).build();

        Complaint complaint = Complaint.builder()
                .complaintId(500)
                .booking(booking)
                .user(user)
                .status(ComplaintStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(complaintRepository.findById(500)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintResponse response = complaintService.resolveComplaintByAdmin(500, request);

        assertNotNull(response);
        assertEquals("resolved", response.getStatus());
        assertTrue(response.getResponses().get(0).getMessage().contains("Phạt chủ sân"));
        assertEquals("Admin", response.getResponses().get(0).getFrom());

        verify(notificationService).createNotification(eq(10), eq("Khiếu nại đã được Admin giải quyết"), anyString(), eq(NotificationType.COMPLAINT), eq("500"));
        verify(notificationService).createNotification(eq(20), eq("Khiếu nại đã được Admin giải quyết"), anyString(), eq(NotificationType.COMPLAINT), eq("500"));
    }

    @Test
    void replyComplaint_ByAdmin_Success() {
        ReplyComplaintRequest request = ReplyComplaintRequest.builder().message("Admin đang xem xét").build();

        Role adminRole = Role.builder().roleId(3).roleName("Admin").build();
        User adminUser = User.builder().userId(30).email("admin@example.com").role(adminRole).build();

        User user = User.builder().userId(10).email("customer@example.com").build();
        User ownerUser = User.builder().userId(20).email("owner@example.com").build();
        com.sportvenue.entity.Owner owner = com.sportvenue.entity.Owner.builder().ownerId(5).user(ownerUser).build();
        com.sportvenue.entity.Stadium stadium = com.sportvenue.entity.Stadium.builder().stadiumId(100).owner(owner).stadiumName("Stadium A").build();
        Booking booking = Booking.builder().bookingId(1).user(user).stadium(stadium).build();

        Complaint complaint = Complaint.builder()
                .complaintId(500)
                .booking(booking)
                .user(user)
                .status(ComplaintStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(complaintRepository.findById(500)).thenReturn(Optional.of(complaint));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintResponse response = complaintService.replyComplaint(500, request, "admin@example.com");

        assertNotNull(response);
        assertEquals("in_progress", response.getStatus());
        assertEquals("Admin", response.getResponses().get(0).getFrom());
        assertEquals("Admin đang xem xét", response.getResponses().get(0).getMessage());

        verify(notificationService).createNotification(eq(10), eq("Admin phản hồi khiếu nại"), anyString(), eq(NotificationType.COMPLAINT), eq("500"));
        verify(notificationService).createNotification(eq(20), eq("Admin phản hồi khiếu nại"), anyString(), eq(NotificationType.COMPLAINT), eq("500"));
    }

    @Test
    void getOwnerComplaints_Success() {
        User ownerUser = User.builder().userId(20).email("owner@example.com").build();
        com.sportvenue.entity.Owner owner = com.sportvenue.entity.Owner.builder().ownerId(5).user(ownerUser).build();
        com.sportvenue.entity.Stadium stadium = com.sportvenue.entity.Stadium.builder().stadiumId(100).owner(owner).stadiumName("Stadium A").build();
        User customer = User.builder().userId(10).email("customer@example.com").build();
        Booking booking = Booking.builder().bookingId(1).user(customer).stadium(stadium).build();

        List<Complaint> mockList = List.of(
                Complaint.builder().complaintId(1).booking(booking).user(customer)
                        .status(ComplaintStatus.OPEN).createdAt(LocalDateTime.now()).build()
        );

        Pageable pageable = PageRequest.of(0, 20);
        when(complaintRepository.findByBookingStadiumOwnerUserEmailOrderByCreatedAtDesc(
                eq("owner@example.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(mockList));

        Page<ComplaintResponse> result = complaintService.getOwnerComplaints("owner@example.com", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().get(0).getComplaintId());
        verify(complaintRepository).findByBookingStadiumOwnerUserEmailOrderByCreatedAtDesc(
                eq("owner@example.com"), any(Pageable.class));
    }

    @Test
    void getCustomerComplaints_Success() {
        User customer = User.builder().userId(10).email("customer@example.com").build();
        User ownerUser = User.builder().userId(20).email("owner@example.com").build();
        com.sportvenue.entity.Owner owner = com.sportvenue.entity.Owner.builder().ownerId(5).user(ownerUser).build();
        com.sportvenue.entity.Stadium stadium = com.sportvenue.entity.Stadium.builder().stadiumId(100).owner(owner).stadiumName("Stadium A").build();
        Booking booking = Booking.builder().bookingId(1).user(customer).stadium(stadium).build();

        List<Complaint> mockList = List.of(
                Complaint.builder().complaintId(2).booking(booking).user(customer)
                        .status(ComplaintStatus.IN_PROGRESS).createdAt(LocalDateTime.now()).build()
        );

        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(complaintRepository.findByUserUserIdOrderByCreatedAtDesc(eq(10), any(Pageable.class)))
                .thenReturn(new PageImpl<>(mockList));

        Page<ComplaintResponse> result = complaintService.getCustomerComplaints("customer@example.com", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(2, result.getContent().get(0).getComplaintId());
        verify(complaintRepository).findByUserUserIdOrderByCreatedAtDesc(eq(10), any(Pageable.class));
    }

    @Test
    void replyComplaint_ByOwner_Success() {
        ReplyComplaintRequest request = ReplyComplaintRequest.builder().message("Chủ sân đang kiểm tra").build();

        User ownerUser = User.builder().userId(20).email("owner@example.com").build();
        com.sportvenue.entity.Owner owner = com.sportvenue.entity.Owner.builder().ownerId(5).user(ownerUser).build();
        com.sportvenue.entity.Stadium stadium = com.sportvenue.entity.Stadium.builder().stadiumId(100).owner(owner).stadiumName("Stadium A").build();
        User customer = User.builder().userId(10).email("customer@example.com").build();
        Booking booking = Booking.builder().bookingId(1).user(customer).stadium(stadium).build();

        Complaint complaint = Complaint.builder()
                .complaintId(500)
                .booking(booking)
                .user(customer)
                .status(ComplaintStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(complaintRepository.findById(500)).thenReturn(Optional.of(complaint));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(20)).thenReturn(Optional.of(owner));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintResponse response = complaintService.replyComplaint(500, request, "owner@example.com");

        assertNotNull(response);
        assertEquals("in_progress", response.getStatus());
        assertEquals("Chủ sân", response.getResponses().get(0).getFrom());
        assertEquals("Chủ sân đang kiểm tra", response.getResponses().get(0).getMessage());

        verify(notificationService).createNotification(eq(10), eq("Phản hồi khiếu nại mới"), anyString(), eq(NotificationType.COMPLAINT), eq("500"));
    }

    @Test
    void replyComplaint_ByCustomer_Success() {
        ReplyComplaintRequest request = ReplyComplaintRequest.builder().message("Tôi cần thêm thông tin").build();

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
        when(ownerRepository.findByUserUserId(10)).thenReturn(Optional.empty());
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintResponse response = complaintService.replyComplaint(500, request, "customer@example.com");

        assertNotNull(response);
        assertEquals("in_progress", response.getStatus());
        assertEquals("Khách hàng", response.getResponses().get(0).getFrom());
        assertEquals("Tôi cần thêm thông tin", response.getResponses().get(0).getMessage());

        verify(notificationService).createNotification(eq(20), eq("Phản hồi khiếu nại mới"), anyString(), eq(NotificationType.COMPLAINT), eq("500"));
    }

    @Test
    void resolveComplaint_ByOwner_Success() {
        ResolveComplaintRequest request = ResolveComplaintRequest.builder().resolution("Đã hoàn tiền cho khách").build();

        User ownerUser = User.builder().userId(20).email("owner@example.com").build();
        com.sportvenue.entity.Owner owner = com.sportvenue.entity.Owner.builder().ownerId(5).user(ownerUser).build();
        com.sportvenue.entity.Stadium stadium = com.sportvenue.entity.Stadium.builder().stadiumId(100).owner(owner).stadiumName("Stadium A").build();
        User customer = User.builder().userId(10).email("customer@example.com").build();
        Booking booking = Booking.builder().bookingId(1).user(customer).stadium(stadium).build();

        Complaint complaint = Complaint.builder()
                .complaintId(500)
                .booking(booking)
                .user(customer)
                .status(ComplaintStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(complaintRepository.findById(500)).thenReturn(Optional.of(complaint));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(ownerRepository.findByUserUserId(20)).thenReturn(Optional.of(owner));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintResponse response = complaintService.resolveComplaint(500, request, "owner@example.com");

        assertNotNull(response);
        assertEquals("resolved", response.getStatus());
        assertEquals("Chủ sân", response.getResponses().get(0).getFrom());
        assertTrue(response.getResponses().get(0).getMessage().contains("Đã hoàn tiền cho khách"));

        verify(notificationService).createNotification(eq(10), eq("Khiếu nại đã được giải quyết"), anyString(), eq(NotificationType.COMPLAINT), eq("500"));
    }
}
