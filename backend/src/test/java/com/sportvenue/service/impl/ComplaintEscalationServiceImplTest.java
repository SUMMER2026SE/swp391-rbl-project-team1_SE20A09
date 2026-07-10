package com.sportvenue.service.impl;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.ComplaintStatus;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ForbiddenException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.ComplaintRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.EmailService;
import com.sportvenue.service.NotificationService;
import com.sportvenue.util.AfterCommitExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintEscalationServiceImplTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    @InjectMocks
    private ComplaintEscalationServiceImpl escalationService;

    private User customer;
    private User otherUser;
    private User admin;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        customer = User.builder().userId(10).email("customer@example.com").build();
        otherUser = User.builder().userId(11).email("other@example.com").build();
        
        Role adminRole = Role.builder().roleId(1).roleName("Admin").build();
        admin = User.builder().userId(1).email("admin@example.com").role(adminRole).build();
        
        complaint = Complaint.builder()
                .complaintId(100)
                .user(customer)
                .status(ComplaintStatus.OPEN)
                .build();
    }

    @Test
    void escalateToAdmin_Success_ByCustomer() {
        when(complaintRepository.findById(100)).thenReturn(Optional.of(complaint));
        when(userRepository.findAllAdmins()).thenReturn(List.of(admin));

        escalationService.escalateToAdmin(100, "Need help", "customer@example.com");

        assertEquals(ComplaintStatus.ESCALATED, complaint.getStatus());
        verify(complaintRepository).save(complaint);
        verify(notificationService).createNotification(
                eq(1), anyString(), anyString(), eq(NotificationType.COMPLAINT), eq("ESCALATED-100"));
    }

    @Test
    void escalateToAdmin_Failure_IDOR() {
        when(complaintRepository.findById(100)).thenReturn(Optional.of(complaint));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThrows(ForbiddenException.class, () -> 
            escalationService.escalateToAdmin(100, "Need help", "other@example.com")
        );
    }

    @Test
    void escalateToAdmin_Success_BySystem() {
        when(complaintRepository.findById(100)).thenReturn(Optional.of(complaint));
        when(userRepository.findAllAdmins()).thenReturn(List.of(admin));

        escalationService.escalateToAdmin(100, "Auto escalate", "SYSTEM");

        assertEquals(ComplaintStatus.ESCALATED, complaint.getStatus());
        verify(complaintRepository).save(complaint);
    }

    @Test
    void startOwnerResolution_Success() {
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        when(complaintRepository.findById(100)).thenReturn(Optional.of(complaint));

        escalationService.startOwnerResolution(100, "Hoàn tiền", "owner@example.com");

        assertEquals(ComplaintStatus.PENDING_ADMIN_REVIEW, complaint.getStatus());
        assertNotNull(complaint.getResolvedAt());
        assertNotNull(complaint.getCustomerResponseDeadline());
        
        verify(complaintRepository).save(complaint);
        verify(notificationService).createNotification(
                eq(10), anyString(), anyString(), eq(NotificationType.COMPLAINT), eq("100"));
        verify(afterCommitExecutor).execute(any(Runnable.class));
    }

    @Test
    void customerObjectToResolution_Success() {
        complaint.setStatus(ComplaintStatus.PENDING_ADMIN_REVIEW);
        complaint.setCustomerResponseDeadline(LocalDateTime.now().plusHours(24));
        when(complaintRepository.findById(100)).thenReturn(Optional.of(complaint));

        escalationService.customerObjectToResolution(100, "Không đồng ý", "customer@example.com");

        assertEquals(ComplaintStatus.ESCALATED, complaint.getStatus());
    }

    @Test
    void customerObjectToResolution_Failure_IDOR() {
        when(complaintRepository.findById(100)).thenReturn(Optional.of(complaint));

        assertThrows(ForbiddenException.class, () -> 
            escalationService.customerObjectToResolution(100, "Không đồng ý", "other@example.com")
        );
    }

    @Test
    void checkSlaViolations_Success() {
        Complaint violation = Complaint.builder()
                .complaintId(200)
                .user(customer)
                .status(ComplaintStatus.OPEN)
                .createdAt(LocalDateTime.now().minusHours(48))
                .build();
                
        when(complaintRepository.findByStatusInAndCreatedAtBeforeAndSlaViolatedFalse(any(), any()))
                .thenReturn(List.of(violation));
        when(complaintRepository.findById(200)).thenReturn(Optional.of(violation));
        when(userRepository.findAllAdmins()).thenReturn(List.of(admin));

        escalationService.checkSlaViolations();

        assertTrue(violation.getSlaViolated());
        assertEquals(ComplaintStatus.ESCALATED, violation.getStatus());
        verify(complaintRepository, times(2)).save(violation);
    }
}
