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

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final ObjectMapper objectMapper;

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

        Complaint complaint = Complaint.builder()
                .booking(booking)
                .user(user)
                .content(request.getDescription())
                .status(ComplaintStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        Complaint saved = complaintRepository.save(complaint);
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

        if (!isOwner && !isCustomer) {
            throw new BadRequestException("Bạn không có quyền tham gia cuộc thảo luận này!");
        }

        // Deserialize existing responses
        List<ComplaintResponse.ResponseItem> items = deserializeResponses(complaint.getResponse());
        
        String fromLabel = isOwner ? "Chủ sân" : "Khách hàng";
        items.add(ComplaintResponse.ResponseItem.builder()
                .from(fromLabel)
                .message(request.getMessage().trim())
                .time(LocalDateTime.now().format(FORMATTER))
                .build());

        complaint.setResponse(serializeResponses(items));
        
        // If it was OPEN, change status to IN_PROGRESS when owner replies
        if (isOwner && complaint.getStatus() == ComplaintStatus.OPEN) {
            complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        }

        Complaint saved = complaintRepository.save(complaint);
        return mapToResponse(saved);
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
        return mapToResponse(saved);
    }

    private ComplaintResponse mapToResponse(Complaint c) {
        String subject = "Khiếu nại đặt sân #" + c.getBooking().getBookingId();
        
        List<ComplaintResponse.ResponseItem> responsesList = deserializeResponses(c.getResponse());
        
        String resolution = null;
        String resolvedDate = null;
        if (c.getStatus() == ComplaintStatus.RESOLVED) {
            resolvedDate = c.getCreatedAt().toLocalDate().toString(); // simple fallback
            if (!responsesList.isEmpty()) {
                resolution = responsesList.get(responsesList.size() - 1).getMessage().replace("Đã giải quyết: ", "");
            } else {
                resolution = "Đã giải quyết xong.";
            }
        }

        return ComplaintResponse.builder()
                .id("CP" + String.format("%03d", c.getComplaintId()))
                .complaintId(c.getComplaintId())
                .subject(subject)
                .against(c.getBooking().getStadium().getStadiumName())
                .description(c.getContent())
                .status(c.getStatus().name().toLowerCase())
                .submittedDate(c.getCreatedAt().toLocalDate().toString())
                .resolvedDate(resolvedDate)
                .resolution(resolution)
                .bookingId(c.getBooking().getBookingId())
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
