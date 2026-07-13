package com.sportvenue.service.impl;

import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.ComplaintStatus;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.ComplaintRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.ComplaintEscalationService;
import com.sportvenue.service.NotificationService;
import com.sportvenue.service.EmailService;
import com.sportvenue.util.AfterCommitExecutor;
import com.sportvenue.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvenue.dto.response.ComplaintResponse;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintEscalationServiceImpl implements ComplaintEscalationService {
    
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AfterCommitExecutor afterCommitExecutor;
    private final ObjectMapper objectMapper;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    // SLA: Owner must respond within 24 hours
    private static final int OWNER_RESPONSE_SLA_HOURS = 24;
    // Customer has 48 hours to object to resolution
    private static final int CUSTOMER_OBJECTION_HOURS = 48;
    
    @Override
    @Transactional
    public void escalateToAdmin(Integer complaintId, String reason, String requestedByEmail) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại ID: " + complaintId));
        
        if (!"SYSTEM".equals(requestedByEmail) && 
            !complaint.getUser().getEmail().equals(requestedByEmail) &&
            !userRepository.findByEmail(requestedByEmail)
                .map(u -> u.getRole() != null && "Admin".equals(u.getRole().getRoleName()))
                .orElse(false)) {
            throw new ForbiddenException("Bạn không có quyền chuyển khiếu nại này");
        }
        
        if (complaint.getStatus() == ComplaintStatus.RESOLVED || 
            complaint.getStatus() == ComplaintStatus.ESCALATED) {
            throw new BadRequestException("Không thể escalate khiếu nại đã giải quyết hoặc đã escalate");
        }
        
        complaint.setStatus(ComplaintStatus.ESCALATED);
        complaint.setEscalatedAt(LocalDateTime.now());
        complaint.setEscalationReason(reason);
        
        complaintRepository.save(complaint);
        
        // Notify all admins
        userRepository.findAllAdmins().forEach(admin ->
            notificationService.createNotification(
                admin.getUserId(),
                "Khiếu nại được chuyển lên",
                String.format("Khiếu nại #%d: %s", complaint.getComplaintId(), reason),
                NotificationType.COMPLAINT,
                "ESCALATED-" + complaint.getComplaintId()
            )
        );
        
        log.info("Complaint {} escalated to admin by {}: {}", complaintId, requestedByEmail, reason);
    }
    
    @Override
    @Transactional
    public void startOwnerResolution(Integer complaintId, String resolution, String ownerEmail) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại ID: " + complaintId));
        
        LocalDateTime now = LocalDateTime.now();
        complaint.setStatus(ComplaintStatus.AWAITING_CUSTOMER_RESPONSE);
        complaint.setResolvedAt(now);
        complaint.setCustomerResponseDeadline(now.plusHours(CUSTOMER_OBJECTION_HOURS));
        
        complaintRepository.save(complaint);
        
        // Notify customer about resolution and 48h objection period
        notificationService.createNotification(
            complaint.getUser().getUserId(),
            "Khiếu nại đã được xử lý",
            String.format("Chủ sân đã xử lý khiếu nại #%d. Bạn có 48h để phản hồi nếu không đồng ý.", 
                         complaint.getComplaintId()),
            NotificationType.COMPLAINT,
            String.valueOf(complaint.getComplaintId())
        );
        
        afterCommitExecutor.execute(() -> emailService.sendComplaintResolvedEmail(
            complaint.getUser().getEmail(),
            complaint.getUser().getFullName(),
            complaint.getComplaintId(),
            resolution
        ));
        
        log.info("Owner {} resolved complaint {}, starting 48h customer objection period", 
                ownerEmail, complaintId);
    }
    
    @Override
    @Transactional
    public void customerObjectToResolution(Integer complaintId, String objectionReason, String customerEmail) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại ID: " + complaintId));
        
        if (!complaint.getUser().getEmail().equals(customerEmail)) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên khiếu nại này");
        }
        
        if (complaint.getStatus() != ComplaintStatus.AWAITING_CUSTOMER_RESPONSE) {
            throw new BadRequestException("Chỉ có thể phản đối trong thời gian chờ xem xét");
        }
        
        if (LocalDateTime.now().isAfter(complaint.getCustomerResponseDeadline())) {
            throw new BadRequestException("Đã hết thời hạn phản đối (48h)");
        }
        
        List<ComplaintResponse.ResponseItem> items = deserializeResponses(complaint.getResponse());
        items.add(ComplaintResponse.ResponseItem.builder()
                .from("Khách hàng")
                .message("Khách hàng phản đối: " + objectionReason)
                .time(LocalDateTime.now().format(FORMATTER))
                .build());
        complaint.setResponse(serializeResponses(items));
        complaintRepository.save(complaint);
        
        // Auto-escalate to admin
        escalateToAdmin(complaintId, "Khách hàng phản đối: " + objectionReason, customerEmail);
    }
    
    @Override
    @Transactional
    public void adminApproveResolution(Integer complaintId, String adminEmail) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại ID: " + complaintId));
        
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy admin: " + adminEmail));
        
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setAdminReviewedBy(admin);
        complaint.setAdminReviewedAt(LocalDateTime.now());
        
        complaintRepository.save(complaint);
        
        // Notify both customer and owner
        notificationService.createNotification(
            complaint.getUser().getUserId(),
            "Khiếu nại đã được xác nhận",
            String.format("Admin đã xác nhận giải pháp cho khiếu nại #%d", complaint.getComplaintId()),
            NotificationType.COMPLAINT,
            String.valueOf(complaint.getComplaintId())
        );
        
        log.info("Admin {} approved resolution for complaint {}", adminEmail, complaintId);
    }
    
    @Override
    @Transactional
    public void adminOverrideResolution(Integer complaintId, String newResolution, String adminEmail) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại ID: " + complaintId));
        
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy admin: " + adminEmail));
        
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResponse(newResolution); // Override owner's resolution
        complaint.setAdminReviewedBy(admin);
        complaint.setAdminReviewedAt(LocalDateTime.now());
        
        complaintRepository.save(complaint);
        
        // Notify both parties
        notificationService.createNotification(
            complaint.getUser().getUserId(),
            "Khiếu nại đã được Admin giải quyết",
            String.format("Admin đã cập nhật giải pháp cho khiếu nại #%d", complaint.getComplaintId()),
            NotificationType.COMPLAINT,
            String.valueOf(complaint.getComplaintId())
        );
        
        notificationService.createNotification(
            complaint.getBooking().getStadium().getOwner().getUser().getUserId(),
            "Admin đã can thiệp khiếu nại",
            String.format("Admin đã thay đổi giải pháp cho khiếu nại #%d", complaint.getComplaintId()),
            NotificationType.COMPLAINT,
            String.valueOf(complaint.getComplaintId())
        );
        
        log.info("Admin {} overrode resolution for complaint {} with: {}", 
                adminEmail, complaintId, newResolution);
    }
    
    @Override
    @Scheduled(fixedDelay = 3600000) // Run every hour
    @Transactional
    public void processCustomerResponseDeadlines() {
        List<Complaint> pendingComplaints = complaintRepository
                .findByStatusAndCustomerResponseDeadlineBefore(
                    ComplaintStatus.AWAITING_CUSTOMER_RESPONSE, 
                    LocalDateTime.now());
        
        for (Complaint complaint : pendingComplaints) {
            try {
                complaint.setStatus(ComplaintStatus.RESOLVED);
                complaintRepository.save(complaint);
                
                // Notify customer that deadline passed
                notificationService.createNotification(
                    complaint.getUser().getUserId(),
                    "Khiếu nại đã được chấp nhận",
                    String.format("Khiếu nại #%d đã được tự động chấp nhận do hết thời hạn phản hồi", 
                                 complaint.getComplaintId()),
                    NotificationType.COMPLAINT,
                    String.valueOf(complaint.getComplaintId())
                );
            } catch (Exception e) {
                log.error("Lỗi khi xử lý processCustomerResponseDeadlines cho khiếu nại {}: {}", complaint.getComplaintId(), e.getMessage());
            }
        }
        
        if (!pendingComplaints.isEmpty()) {
            log.info("Auto-finalized {} complaints after customer response deadline", 
                    pendingComplaints.size());
        }
    }
    
    @Override
    @Scheduled(fixedDelay = 3600000) // Run every hour  
    @Transactional
    public void checkSlaViolations() {
        LocalDateTime slaThreshold = LocalDateTime.now().minusHours(OWNER_RESPONSE_SLA_HOURS);
        
        List<Complaint> violations = complaintRepository
                .findByStatusInAndCreatedAtBeforeAndSlaViolatedFalse(
                    List.of(ComplaintStatus.OPEN, ComplaintStatus.IN_PROGRESS), 
                    slaThreshold);
        
        for (Complaint complaint : violations) {
            try {
                complaint.setSlaViolated(true);
                complaintRepository.save(complaint);
                
                // Auto-escalate SLA violations
                escalateToAdmin(complaint.getComplaintId(), 
                              "Tự động escalate - vi phạm SLA " + OWNER_RESPONSE_SLA_HOURS + "h", 
                              "SYSTEM");
            } catch (Exception e) {
                log.error("Lỗi khi xử lý checkSlaViolations cho khiếu nại {}: {}", complaint.getComplaintId(), e.getMessage());
            }
        }
        
        if (!violations.isEmpty()) {
            log.info("Found and escalated {} SLA violations", violations.size());
        }
    }
    
    private List<ComplaintResponse.ResponseItem> deserializeResponses(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ComplaintResponse.ResponseItem>>() { });
        } catch (Exception e) {
            log.warn("Failed to deserialize complaint response JSON: {}", json, e);
            return new ArrayList<>();
        }
    }

    private String serializeResponses(List<ComplaintResponse.ResponseItem> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Failed to serialize complaint response list to JSON", e);
            return "[]";
        }
    }
}