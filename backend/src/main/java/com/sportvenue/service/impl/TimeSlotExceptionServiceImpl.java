package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateExceptionRequest;
import com.sportvenue.dto.response.TimeSlotExceptionResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.TimeSlotException;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.TimeSlotExceptionRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.service.TimeSlotExceptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotExceptionServiceImpl implements TimeSlotExceptionService {

    private final TimeSlotExceptionRepository timeSlotExceptionRepository;
    private final TimeSlotRepository timeSlotRepository;

    @Override
    @Transactional
    public TimeSlotExceptionResponse createOrUpdateException(Integer slotId, LocalDate date, CreateExceptionRequest request, Integer userId) {
        TimeSlot slot = timeSlotRepository.findByIdWithOwner(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung giờ"));

        Stadium stadium = slot.getStadium();
        // Validate ownership
        Owner resolvedOwner = stadium.resolveOwner();
        if (resolvedOwner == null || resolvedOwner.getUser() == null ||
                !resolvedOwner.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa ngoại lệ cho khung giờ này");
        }

        TimeSlotException exception = timeSlotExceptionRepository.findBySlotSlotIdAndExceptionDate(slotId, date)
                .orElse(TimeSlotException.builder()
                        .slot(slot)
                        .exceptionDate(date)
                        .build());

        exception.setPriceOverride(request.getPriceOverride());
        exception.setStartTimeOverride(request.getStartTimeOverride());
        exception.setEndTimeOverride(request.getEndTimeOverride());
        exception.setClosed(request.getClosed() != null ? request.getClosed() : false);
        exception.setHidden(request.getHidden() != null ? request.getHidden() : false);

        // [SUGGEST] Cross-field validation: giờ kết thúc phải sau giờ bắt đầu
        if (request.getStartTimeOverride() != null && request.getEndTimeOverride() != null
                && !request.getStartTimeOverride().isBefore(request.getEndTimeOverride())) {
            throw new BadRequestException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        // [SUGGEST] Đặt giá đè cho slot đã đóng là vô nghĩa
        if (Boolean.TRUE.equals(request.getClosed()) && request.getPriceOverride() != null) {
            throw new BadRequestException("Không thể đặt giá ghi đè cho khung giờ đã đóng");
        }

        TimeSlotException saved = timeSlotExceptionRepository.save(exception);
        
        log.info("ℹ️ TimeSlotException updated for slot {} on date {} — priceOverride={}, startTimeOverride={}, endTimeOverride={}, closed={}, hidden={}",
                slotId, date, saved.getPriceOverride(), saved.getStartTimeOverride(), saved.getEndTimeOverride(), saved.getClosed(), saved.getHidden());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteException(Integer slotId, LocalDate date, Integer userId) {
        TimeSlot slot = timeSlotRepository.findByIdWithOwner(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung giờ"));

        Stadium stadium = slot.getStadium();
        // Validate ownership
        Owner resolvedOwner = stadium.resolveOwner();
        if (resolvedOwner == null || resolvedOwner.getUser() == null ||
                !resolvedOwner.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xóa ngoại lệ cho khung giờ này");
        }

        TimeSlotException exception = timeSlotExceptionRepository.findBySlotSlotIdAndExceptionDate(slotId, date)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình ngoại lệ cho ngày này"));

        timeSlotExceptionRepository.delete(exception);
        log.info("ℹ️ TimeSlotException deleted for slot {} on date {}", slotId, date);
    }

    private TimeSlotExceptionResponse toResponse(TimeSlotException exception) {
        return TimeSlotExceptionResponse.builder()
                .exceptionId(exception.getExceptionId())
                .slotId(exception.getSlot().getSlotId())
                .exceptionDate(exception.getExceptionDate())
                .priceOverride(exception.getPriceOverride())
                .startTimeOverride(exception.getStartTimeOverride())
                .endTimeOverride(exception.getEndTimeOverride())
                .closed(exception.getClosed())
                .hidden(exception.getHidden())
                .build();
    }
}
