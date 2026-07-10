package com.sportvenue.service;

import com.sportvenue.dto.request.CreateComplaintRequest;
import com.sportvenue.dto.request.ReplyComplaintRequest;
import com.sportvenue.dto.request.ResolveComplaintRequest;
import com.sportvenue.dto.response.ComplaintResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ComplaintService {

    /** Lấy toàn bộ khiếu nại của các sân thuộc quản lý của một Owner. */
    Page<ComplaintResponse> getOwnerComplaints(String ownerEmail, Pageable pageable);

    /** Lấy danh sách khiếu nại của chính khách hàng đăng nhập. */
    Page<ComplaintResponse> getCustomerComplaints(String customerEmail, Pageable pageable);

    /** Khách hàng gửi khiếu nại mới. */
    ComplaintResponse createComplaint(CreateComplaintRequest request, String customerEmail);

    /** Owner hoặc Customer nhắn phản hồi trong khiếu nại. */
    ComplaintResponse replyComplaint(Integer complaintId, ReplyComplaintRequest request, String userEmail);

    /** Owner đóng giải quyết khiếu nại. */
    ComplaintResponse resolveComplaint(Integer complaintId, ResolveComplaintRequest request, String ownerEmail);

    /** Khách hàng tự đóng khiếu nại của mình. */
    ComplaintResponse closeComplaint(Integer complaintId, String customerEmail);

    /** Admin lấy toàn bộ khiếu nại trong hệ thống. */
    Page<ComplaintResponse> getAllComplaints(Pageable pageable);

    /** Admin đóng giải quyết khiếu nại. */
    ComplaintResponse resolveComplaintByAdmin(Integer complaintId, ResolveComplaintRequest request);

    /** Admin lấy danh sách khiếu nại đã escalate hoặc chờ xem xét. */
    Page<ComplaintResponse> getEscalatedComplaints(Pageable pageable);
}
