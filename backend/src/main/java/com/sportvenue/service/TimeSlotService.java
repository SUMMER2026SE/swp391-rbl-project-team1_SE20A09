package com.sportvenue.service;

import com.sportvenue.dto.request.CreateTimeSlotRequest;
import com.sportvenue.dto.response.TimeSlotResponse;

import java.util.List;

public interface TimeSlotService {
    List<TimeSlotResponse> getSlotsByStadiumId(Integer stadiumId);
    TimeSlotResponse createSlot(Integer stadiumId, CreateTimeSlotRequest request, Integer userId);
    List<TimeSlotResponse> bulkCreateSlots(Integer stadiumId, List<CreateTimeSlotRequest> requests, Integer userId);
    void deleteSlot(Integer slotId, Integer userId);
    TimeSlotResponse toggleSlotStatus(Integer slotId, Integer userId);
}
