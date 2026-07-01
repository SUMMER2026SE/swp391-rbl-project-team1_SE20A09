package com.sportvenue.service;

import com.sportvenue.dto.request.BulkTimeSlotRequest;
import com.sportvenue.dto.response.TimeSlotResponse;

import java.util.List;

public interface ComplexTimeSlotService {
    /**
     * Tạo khung giờ hàng loạt (Bulk) cho các sân lẻ thuộc một Khu (Facility).
     */
    List<TimeSlotResponse> bulkCreateSlotsForFacility(Integer facilityId, BulkTimeSlotRequest request, Integer userId);

    /**
     * Tạo khung giờ hàng loạt (Bulk) cho các sân lẻ thuộc một Tổ hợp (Complex).
     */
    List<TimeSlotResponse> bulkCreateSlotsForComplex(Integer complexId, BulkTimeSlotRequest request, Integer userId);
}
