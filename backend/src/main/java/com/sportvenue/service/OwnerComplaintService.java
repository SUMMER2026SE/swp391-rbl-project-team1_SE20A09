package com.sportvenue.service;

import com.sportvenue.dto.request.ComplaintReplyRequest;
import com.sportvenue.dto.response.ComplaintResponse;
import com.sportvenue.entity.Complaint;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.ComplaintStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.ComplaintRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.StadiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service xử lý nghiệp vụ khiếu nại cho Owner.
 * UC-OWN-09: Xem và phản hồi khiếu nại của khách hàng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerComplaintService {

    private final ComplaintRepository complaintRepository;
    private final OwnerRepository ownerRepository;
    private final StadiumRepository stadiumRepository;

    /**
     * Lấy danh sách khiếu nại của tất cả sân thuộc Owner.
     *
     * @param userId ID của Owner
     * @param stadiumId (optional) filter theo sân cụ thể
     * @param pageable phân trang
     * @return trang complaint responses
     */
    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getOwnerComplaints(
            Integer userId, Integer stadiumId, Pageable pageable) {

        Owner owner = findOwnerByUserId(userId);

        if (stadiumId != null) {
            validateStadiumOwnership(stadiumId, owner.getOwnerId());
            return complaintRepository
                    .findByBookingStadiumStadiumIdOrderByCreatedAtDesc(
                            stadiumId, pageable)
                    .map(this::toComplaintResponse);
        }

        // Lấy tất cả complaints từ tất cả sân của owner
        List<Stadium> stadiums = stadiumRepository
                .findByOwnerOwnerIdAndStadiumStatusNot(
                        owner.getOwnerId(),
                        com.sportvenue.entity.enums.StadiumStatus.CLOSED);
        List<Integer> stadiumIds = stadiums.stream()
                .map(Stadium::getStadiumId)
                .toList();

        if (stadiumIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return complaintRepository
                .findByBookingStadiumStadiumIdInOrderByCreatedAtDesc(
                        stadiumIds, pageable)
                .map(this::toComplaintResponse);
    }

    /**
     * UC-OWN-09: Owner phản hồi và giải quyết khiếu nại.
     * Trạng thái complaint sẽ chuyển từ OPEN/IN_PROGRESS → RESOLVED.
     *
     * @param userId ID của Owner
     * @param complaintId ID của complaint cần xử lý
     * @param request nội dung phản hồi
     * @return complaint response sau khi cập nhật
     */
    @Transactional
    public ComplaintResponse resolveComplaint(
            Integer userId, Integer complaintId,
            ComplaintReplyRequest request) {

        Owner owner = findOwnerByUserId(userId);
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy khiếu nại: " + complaintId));

        // Kiểm tra quyền sở hữu sân liên quan
        validateStadiumOwnership(
                complaint.getBooking().getStadium().getStadiumId(),
                owner.getOwnerId());

        // Kiểm tra khiếu nại đã giải quyết chưa
        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new BadRequestException(
                    "Khiếu nại này đã được giải quyết trước đó.");
        }

        complaint.setResponse(request.getResponse());
        complaint.setStatus(ComplaintStatus.RESOLVED);

        Complaint saved = complaintRepository.save(complaint);
        log.info("🔧 Complaint #{} đã được giải quyết bởi Owner",
                saved.getComplaintId());
        return toComplaintResponse(saved);
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
        if (!stadium.getOwner().getOwnerId().equals(ownerId)) {
            throw new BadRequestException(
                    "Bạn không có quyền quản lý sân này.");
        }
    }

    private ComplaintResponse toComplaintResponse(Complaint complaint) {
        User complainer = complaint.getUser();
        Stadium stadium = complaint.getBooking().getStadium();

        return ComplaintResponse.builder()
                .complaintId(complaint.getComplaintId())
                .bookingId(complaint.getBooking().getBookingId())
                .complainer(ComplaintResponse.ComplainerInfo.builder()
                        .userId(complainer.getUserId())
                        .fullName(complainer.getLastName()
                                + " " + complainer.getFirstName())
                        .email(complainer.getEmail())
                        .phoneNumber(complainer.getPhoneNumber())
                        .build())
                .stadium(ComplaintResponse.StadiumSummary.builder()
                        .stadiumId(stadium.getStadiumId())
                        .stadiumName(stadium.getStadiumName())
                        .build())
                .content(complaint.getContent())
                .status(complaint.getStatus().name())
                .response(complaint.getResponse())
                .createdAt(complaint.getCreatedAt())
                .build();
    }
}
