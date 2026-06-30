package com.sportvenue.service;

import com.sportvenue.dto.request.CreateExceptionRequest;
import com.sportvenue.dto.response.TimeSlotExceptionResponse;

import java.time.LocalDate;

public interface TimeSlotExceptionService {

    TimeSlotExceptionResponse createOrUpdateException(Integer slotId, LocalDate date, CreateExceptionRequest request, Integer userId);

    void deleteException(Integer slotId, LocalDate date, Integer userId);
}
