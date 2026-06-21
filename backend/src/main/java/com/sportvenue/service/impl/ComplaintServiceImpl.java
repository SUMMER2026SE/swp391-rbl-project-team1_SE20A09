package com.sportvenue.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvenue.dto.request.CreateComplaintRequest;
import com.sportvenue.dto.request.ReplyComplaintRequest;
import com.sportvenue.dto.request.ResolveComplaintRequest;
import com.sportvenue.dto.response.ComplaintResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.ComplaintPriority;
import com.sportvenue.entity.enums.ComplaintStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.ComplaintRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sportvenue.dto.response.ComplaintChatEventDTO;
import com.sportvenue.service.NotificationService;
import com.sportvenue.entity.enums.NotificationType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional(readOnly = true)
    @Override
    public List<ComplaintResponse> getOwnerComplaints(String ownerEmail) {
        log.info("Fetching complaints for owner: {}", ownerEmail);
        List<Complaint> complaints = complaintRepository.findByBookingStadiumOwnerUserEmailOrderByCreatedAtDesc(ownerEmail);
        return complaints.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<ComplaintResponse> getCustomerComplaints(String customerEmail) {
        log.info("Fetching complaints for customer: {}", customerEmail);
        User user = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + customerEmail));
        List<Complaint> complaints = complaintRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());
        return complaints.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ComplaintResponse createComplaint(CreateComplaintRequest request, String customerEmail) {
        log.info("Customer {} is creating complaint for bookingId: {}", customerEmail, request.getBookingId());
        
        User user = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + customerEmail));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân ID: " + request.getBookingId()));

        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new BadRequestException("Bạn không có quyền khiếu nại đơn đặt sân này!");
        }

        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Bạn chỉ có thể khiếu nại đơn đặt sân đã hoàn thành!");
        }

        if (complaintRepository.existsByBookingBookingId(request.getBookingId())) {
            throw new BadRequestException("Đơn đặt sân này đã được khiếu nại trước đó!");
        }

        Complaint complaint = Complaint.builder()
                .booking(booking)
                .user(user)
                .subject(request.getSubject().trim())
                .content(request.getDescription())
                .priority(ComplaintPriority.MEDIUM)
                .status(ComplaintStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        Complaint saved = complaintRepository.save(complaint);
        
        // Notify owner
        notificationService.createNotification(
                booking.getStadium().getOwner().getUser().getUserId(),
                "Khiếu nại mới",
                "Khách hàng " + user.getFullName() + " vừa tạo khiếu nại cho đơn đặt sân #" + booking.getBookingId(),
                NotificationType.COMPLAINT,
                String.valueOf(saved.getComplaintId())
        );
        
        return mapToResponse(saved);
    }

    @Transactional
    @Override
    public ComplaintResponse replyComplaint(Integer complaintId, ReplyComplaintRequest request, String userEmail) {
        log.info("Replying to complaintId: {} by user: {}", complaintId, userEmail);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại ID: " + complaintId));

        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new BadRequestException("Không thể phản hồi khiếu nại đã giải quyết xong.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + userEmail));

        boolean isOwner = false;
        Owner owner = ownerRepository.findByUserUserId(user.getUserId()).orElse(null);
        if (owner != null && complaint.getBooking().getStadium().getOwner().getOwnerId().equals(owner.getOwnerId())) {
            isOwner = true;
        }

        boolean isCustomer = complaint.getUser().getUserId().equals(user.getUserId());
        boolean isAdmin = user.getRole() != null && "Admin".equalsIgnoreCase(user.getRole().getRoleName());

        if (!isOwner && !isCustomer && !isAdmin) {
            throw new BadRequestException("Bạn không có quyền tham gia cuộc thảo luận này!");
        }

        // Deserialize existing responses
        List<ComplaintResponse.ResponseItem> items = deserializeResponses(complaint.getResponse());
        
        String fromLabel = "Khách hàng";
        if (isOwner) {
            fromLabel = "Chủ sân";
        } else if (isAdmin) {
            fromLabel = "Admin";
        }

        items.add(ComplaintResponse.ResponseItem.builder()
                .from(fromLabel)
                .message(request.getMessage().trim())
                .time(LocalDateTime.now().format(FORMATTER))
                .build());

        complaint.setResponse(serializeResponses(items));
        
        // If it was OPEN, change status to IN_PROGRESS when owner or admin replies
        if ((isOwner || isAdmin) && complaint.getStatus() == ComplaintStatus.OPEN) {
            complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        }

        Complaint saved = complaintRepository.save(complaint);
        sendReplyNotifications(complaint, saved, isAdmin, isCustomer, isOwner);

        messagingTemplate.convertAndSend(
                "/topic/complaint/" + saved.getComplaintId(),
                ComplaintChatEventDTO.builder()
                        .complaintId(saved.getComplaintId())
                        .from(fromLabel)
                        .message(request.getMessage().trim())
                        .time(LocalDateTime.now().format(FORMATTER))
                        .newStatus(saved.getStatus().name().toLowerCase())
                        .build()
        );

        return mapToResponse(saved);
    }

    private void sendReplyNotifications(Complaint complaint, Complaint saved,
                                        boolean isAdmin, boolean isCustomer, boolean isOwner) {
        String ref = String.valueOf(saved.getComplaintId());
        Integer customerId = complaint.getUser().getUserId();
        Integer ownerId = complaint.getBooking().getStadium().getOwner().getUser().getUserId();
        Integer id = complaint.getComplaintId();
        if (isAdmin) {
            notificationService.createNotification(customerId,
                    "Admin phản hồi khiếu nại", "Admin vừa phản hồi trong khiếu nại #" + id,
                    NotificationType.COMPLAINT, ref);
            notificationService.createNotification(ownerId,
                    "Admin phản hồi khiếu nại", "Admin vừa phản hồi trong khiếu nại #" + id,
                    NotificationType.COMPLAINT, ref);
        } else if (isCustomer) {
            notificationService.createNotification(ownerId,
                    "Phản hồi khiếu nại mới", "Khách hàng vừa phản hồi trong khiếu nại #" + id,
                    NotificationType.COMPLAINT, ref);
        } else if (isOwner) {
            notificationService.createNotification(customerId,
                    "Phản hồi khiếu nại mới", "Chủ sân vừa phản hồi trong khiếu nại #" + id,
                    NotificationType.COMPLAINT, ref);
        }
    }

    @Transactional
    @Override
    public ComplaintResponse resolveComplaint(Integer complaintId, ResolveComplaintRequest request, String ownerEmail) {
        log.info("Resolving complaintId: {} by owner: {}", complaintId, ownerEmail);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại ID: " + complaintId));

        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + ownerEmail));

        Owner owner = ownerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không có profile chủ sân (Owner)"));

        if (!complaint.getBooking().getStadium().getOwner().getOwnerId().equals(owner.getOwnerId())) {
            throw new BadRequestException("Bạn không có quyền giải quyết khiếu nại của sân này!");
        }

        List<ComplaintResponse.ResponseItem> items = deserializeResponses(complaint.getResponse());
        items.add(ComplaintResponse.ResponseItem.builder()
                .from("Chủ sân")
                .message("Đã giải quyết: " + request.getResolution().trim())
                .time(LocalDateTime.now().format(FORMATTER))
                .build());

        complaint.setResponse(serializeResponses(items));
        complaint.setStatus(ComplaintStatus.RESOLVED);

        Complaint saved = complaintRepository.save(complaint);

        notificationService.createNotification(
                complaint.getUser().getUserId(),
                "Khiếu nại đã được giải quyết",
                "Chủ sân đã giải quyết khiếu nại #" + complaintId,
                NotificationType.COMPLAINT,
                String.valueOf(saved.getComplaintId())
        );

        messagingTemplate.convertAndSend(
                "/topic/complaint/" + saved.getComplaintId(),
                ComplaintChatEventDTO.builder()
                        .complaintId(saved.getComplaintId())
                        .from("Chủ sân")
                        .message("Đã giải quyết: " + request.getResolution().trim())
                        .time(LocalDateTime.now().format(FORMATTER))
                        .newStatus("resolved")
                        .build()
        );

        return mapToResponse(saved);
    }

    @Transactional
    @Override
    public ComplaintResponse closeComplaint(Integer complaintId, String customerEmail) {
        log.info("Customer {} closing complaintId: {}", customerEmail, complaintId);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại ID: " + complaintId));

        User user = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + customerEmail));

        if (!complaint.getUser().getUserId().equals(user.getUserId())) {
            throw new BadRequestException("Bạn không có quyền đóng khiếu nại này!");
        }

        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new BadRequestException("Khiếu nại này đã được giải quyết trước đó!");
        }

        List<ComplaintResponse.ResponseItem> items = deserializeResponses(complaint.getResponse());
        items.add(ComplaintResponse.ResponseItem.builder()
                .from("Khách hàng")
                .message("Khách hàng đã đóng khiếu nại.")
                .time(LocalDateTime.now().format(FORMATTER))
                .build());

        complaint.setResponse(serializeResponses(items));
        complaint.setStatus(ComplaintStatus.RESOLVED);

        Complaint saved = complaintRepository.save(complaint);

        notificationService.createNotification(
                complaint.getBooking().getStadium().getOwner().getUser().getUserId(),
                "Khiếu nại đã được đóng",
                "Khách hàng đã tự đóng khiếu nại #" + complaintId,
                NotificationType.COMPLAINT,
                String.valueOf(saved.getComplaintId())
        );

        messagingTemplate.convertAndSend(
                "/topic/complaint/" + saved.getComplaintId(),
                ComplaintChatEventDTO.builder()
                        .complaintId(saved.getComplaintId())
                        .from("Khách hàng")
                        .message("Khách hàng đã đóng khiếu nại.")
                        .time(LocalDateTime.now().format(FORMATTER))
                        .newStatus("resolved")
                        .build()
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ComplaintResponse> getAllComplaints() {
        log.info("Admin fetching all complaints");
        List<Complaint> complaints = complaintRepository.findAllByOrderByCreatedAtDesc();
        return complaints.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ComplaintResponse resolveComplaintByAdmin(Integer complaintId, ResolveComplaintRequest request) {
        log.info("Resolving complaintId: {} by Admin", complaintId);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại ID: " + complaintId));

        List<ComplaintResponse.ResponseItem> items = deserializeResponses(complaint.getResponse());
        items.add(ComplaintResponse.ResponseItem.builder()
                .from("Admin")
                .message("Đã giải quyết: " + request.getResolution().trim())
                .time(LocalDateTime.now().format(FORMATTER))
                .build());

        complaint.setResponse(serializeResponses(items));
        complaint.setStatus(ComplaintStatus.RESOLVED);

        Complaint saved = complaintRepository.save(complaint);

        notificationService.createNotification(
                complaint.getUser().getUserId(),
                "Khiếu nại đã được Admin giải quyết",
                "Admin đã đóng và giải quyết khiếu nại #" + complaintId,
                NotificationType.COMPLAINT,
                String.valueOf(saved.getComplaintId())
        );

        notificationService.createNotification(
                complaint.getBooking().getStadium().getOwner().getUser().getUserId(),
                "Khiếu nại đã được Admin giải quyết",
                "Admin đã đóng và giải quyết khiếu nại #" + complaintId,
                NotificationType.COMPLAINT,
                String.valueOf(saved.getComplaintId())
        );

        messagingTemplate.convertAndSend(
                "/topic/complaint/" + saved.getComplaintId(),
                ComplaintChatEventDTO.builder()
                        .complaintId(saved.getComplaintId())
                        .from("Admin")
                        .message("Đã giải quyết: " + request.getResolution().trim())
                        .time(LocalDateTime.now().format(FORMATTER))
                        .newStatus("resolved")
                        .build()
        );

        return mapToResponse(saved);
    }

    private ComplaintResponse mapToResponse(Complaint c) {
        String subject = c.getSubject();
        if (subject == null) {
            subject = "Khiếu nại đặt sân #" + c.getBooking().getBookingId();
        }
        
        List<ComplaintResponse.ResponseItem> responsesList = deserializeResponses(c.getResponse());
        
        String resolution = null;
        String resolvedDate = null;
        if (c.getStatus() == ComplaintStatus.RESOLVED) {
            if (!responsesList.isEmpty()) {
                String lastTime = responsesList.get(responsesList.size() - 1).getTime();
                resolvedDate = (lastTime != null && lastTime.length() >= 10) ? lastTime.substring(0, 10) : c.getCreatedAt().toLocalDate().toString();
                resolution = responsesList.get(responsesList.size() - 1).getMessage().replace("Đã giải quyết: ", "");
            } else {
                resolvedDate = c.getCreatedAt().toLocalDate().toString();
                resolution = "Đã giải quyết xong.";
            }
        }

        String customerName = c.getUser() != null ? c.getUser().getFullName() : "N/A";
        String customerEmail = c.getUser() != null ? c.getUser().getEmail() : "N/A";
        
        String stadiumName = "N/A";
        Integer stadiumId = null;
        String ownerName = "N/A";
        String ownerEmail = "N/A";
        String bookingStatus = "N/A";
        
        if (c.getBooking() != null) {
            bookingStatus = c.getBooking().getBookingStatus() != null ? c.getBooking().getBookingStatus().name() : "N/A";
            if (c.getBooking().getStadium() != null) {
                var stadium = c.getBooking().getStadium();
                stadiumName = stadium.getStadiumName();
                stadiumId = stadium.getStadiumId();
                if (stadium.getOwner() != null && stadium.getOwner().getUser() != null) {
                    ownerName = stadium.getOwner().getUser().getFullName();
                    ownerEmail = stadium.getOwner().getUser().getEmail();
                }
            }
        }

        return ComplaintResponse.builder()
                .id("CP" + String.format("%03d", c.getComplaintId()))
                .complaintId(c.getComplaintId())
                .subject(subject)
                .against(stadiumName)
                .description(c.getContent())
                .status(c.getStatus().name().toLowerCase())
                .priority(c.getPriority() != null ? c.getPriority().name().toLowerCase() : "medium")
                .submittedDate(c.getCreatedAt().toLocalDate().toString())
                .resolvedDate(resolvedDate)
                .resolution(resolution)
                .bookingId(c.getBooking().getBookingId())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .stadiumName(stadiumName)
                .stadiumId(stadiumId)
                .ownerName(ownerName)
                .ownerEmail(ownerEmail)
                .bookingStatus(bookingStatus)
                .responses(responsesList)
                .build();
    }

    private List<ComplaintResponse.ResponseItem> deserializeResponses(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ComplaintResponse.ResponseItem>>() { });
        } catch (Exception e) {
            log.warn("Failed to deserialize complaint response JSON: {}", json, e);
            // Fallback for non-JSON string formats
            List<ComplaintResponse.ResponseItem> fallbackList = new ArrayList<>();
            fallbackList.add(ComplaintResponse.ResponseItem.builder()
                    .from("Chủ sân")
                    .message(json)
                    .time(LocalDateTime.now().format(FORMATTER))
                    .build());
            return fallbackList;
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
