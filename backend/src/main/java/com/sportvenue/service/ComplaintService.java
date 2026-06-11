package com.sportvenue.service;

import com.sportvenue.dto.request.CreateComplaintRequest;
import com.sportvenue.dto.request.ReplyComplaintRequest;
import com.sportvenue.dto.request.ResolveComplaintRequest;
import com.sportvenue.dto.response.ComplaintResponse;

import java.util.List;

public interface ComplaintService {
    
    /** Lấy toàn bộ khiếu nại của các sân thuộc quản lý của một Owner. */
    List<ComplaintResponse> getOwnerComplaints(String ownerEmail);
    
    /** Lấy danh sách khiếu nại của chính khách hàng đăng nhập. */
    List<ComplaintResponse> getCustomerComplaints(String customerEmail);
    
    /** Khách hàng gửi khiếu nại mới. */
    ComplaintResponse createComplaint(CreateComplaintRequest request, String customerEmail);
    
    /** Owner hoặc Customer nhắn phản hồi trong khiếu nại. */
    ComplaintResponse replyComplaint(Integer complaintId, ReplyComplaintRequest request, String userEmail);
    
    /** Owner đóng giải quyết khiếu nại. */
    ComplaintResponse resolveComplaint(Integer complaintId, ResolveComplaintRequest request, String ownerEmail);
}
