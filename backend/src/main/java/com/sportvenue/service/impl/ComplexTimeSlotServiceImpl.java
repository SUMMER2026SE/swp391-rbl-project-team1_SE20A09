package com.sportvenue.service.impl;

import com.sportvenue.dto.request.BulkTimeSlotRequest;
import com.sportvenue.dto.request.CreateTimeSlotRequest;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.mapper.TimeSlotMapper;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.service.ComplexTimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplexTimeSlotServiceImpl implements ComplexTimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final StadiumRepository stadiumRepository;
    private final StadiumComplexRepository stadiumComplexRepository;
    private final TimeSlotMapper timeSlotMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TimeSlotResponse> bulkCreateSlotsForFacility(Integer facilityId, BulkTimeSlotRequest request, Integer userId) {
        log.info("Bulk creating slots for facility ID: {} by user ID: {}", facilityId, userId);

        // 1. Kiểm tra Facility tồn tại và thuộc loại node FACILITY
        Stadium facility = stadiumRepository.findFacilityWithComplexDetails(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Khu vực sân không tồn tại"));

        if (facility.getNodeType() != StadiumNodeType.FACILITY) {
            throw new BadRequestException("ID cung cấp không phải là một Khu vực (FACILITY)");
        }

        // 2. Kiểm tra quyền sở hữu
        validateFacilityOwnership(facility, userId);

        // 3. Thu thập danh sách sân lẻ (COURT) cần áp dụng
        List<Stadium> targetCourts;
        if (Boolean.TRUE.equals(request.getApplyToAllCourts())) {
            targetCourts = stadiumRepository.findCourtsByFacilityId(facilityId);
        } else {
            if (request.getCourtIds() == null || request.getCourtIds().isEmpty()) {
                throw new BadRequestException("Danh sách courtIds không được để trống khi không chọn applyToAllCourts");
            }
            targetCourts = stadiumRepository.findCourtsByIds(request.getCourtIds());
            // Validate các sân lẻ này thực sự thuộc Facility
            for (Stadium court : targetCourts) {
                if (court.getNodeType() != StadiumNodeType.COURT) {
                    throw new BadRequestException("ID " + court.getStadiumId() + " không phải là Sân lẻ (COURT)");
                }
                if (court.getParentStadium() == null || !court.getParentStadium().getStadiumId().equals(facilityId)) {
                    throw new BadRequestException("Sân lẻ ID " + court.getStadiumId() + " không thuộc Khu vực " + facilityId);
                }
            }
        }

        if (targetCourts.isEmpty()) {
            throw new BadRequestException("Không tìm thấy sân lẻ nào khả dụng để áp dụng");
        }

        // 4. Validate các slots đầu vào (kiểm tra overlap nội bộ trước)
        List<CreateTimeSlotRequest> sortedSlots = validateAndSortRequestSlots(request.getSlots());

        // 5. Kiểm tra trùng lặp và lưu trữ (All-or-Nothing)
        List<TimeSlot> slotsToSave = new ArrayList<>();
        for (Stadium court : targetCourts) {
            // Lấy giờ hoạt động thừa kế hoặc ghi đè
            LocalTime openTime = court.getOpenTime() != null ? court.getOpenTime() : facility.getOpenTime();
            LocalTime closeTime = court.getCloseTime() != null ? court.getCloseTime() : facility.getCloseTime();

            if (openTime == null || closeTime == null) {
                throw new BadRequestException("Sân lẻ hoặc Khu vực cha chưa cấu hình giờ mở/đóng cửa hoạt động");
            }

            for (CreateTimeSlotRequest slotReq : sortedSlots) {
                // Kiểm tra giờ hoạt động
                if (slotReq.getStartTime().isBefore(openTime) || slotReq.getEndTime().isAfter(closeTime)) {
                    throw new BadRequestException(String.format(
                            "Khung giờ %s-%s nằm ngoài giờ hoạt động (%s-%s) của sân lẻ ID: %s",
                            slotReq.getStartTime(), slotReq.getEndTime(),
                            openTime, closeTime, court.getStadiumId()));
                }

                // Kiểm tra overlap trên Database
                List<TimeSlot> overlapping = timeSlotRepository.findOverlappingSlots(
                        court.getStadiumId(), slotReq.getStartTime(), slotReq.getEndTime());
                if (!overlapping.isEmpty()) {
                    throw new BadRequestException(String.format(
                            "Khung giờ %s-%s bị trùng với lịch hiện tại trên sân lẻ ID: %s (%s)",
                            slotReq.getStartTime(), slotReq.getEndTime(),
                            court.getStadiumId(), court.getStadiumName()));
                }

                TimeSlot timeSlot = timeSlotMapper.toEntity(slotReq);
                timeSlot.setStadium(court);
                timeSlot.setSlotStatus(SlotStatus.AVAILABLE);
                slotsToSave.add(timeSlot);
            }
        }

        List<TimeSlot> saved = timeSlotRepository.saveAll(slotsToSave);
        return saved.stream()
                .map(timeSlotMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TimeSlotResponse> bulkCreateSlotsForComplex(Integer complexId, BulkTimeSlotRequest request, Integer userId) {
        log.info("Bulk creating slots for complex ID: {} by user ID: {}", complexId, userId);

        // 1. Kiểm tra Complex tồn tại
        StadiumComplex complex = stadiumComplexRepository.findWithDetailsByComplexId(complexId)
                .orElseThrow(() -> new ResourceNotFoundException("Tổ hợp sân không tồn tại"));

        // 2. Kiểm tra quyền sở hữu
        if (!complex.getOwner().getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền quản lý khung giờ cho tổ hợp này");
        }

        // 3. Thu nhập danh sách sân lẻ thuộc Complex
        List<Stadium> targetCourts = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getApplyToAllCourts())) {
            targetCourts = stadiumRepository.findCourtsByComplexId(complexId);
        } else {
            // Gom nhóm từ courtIds
            if (request.getCourtIds() != null && !request.getCourtIds().isEmpty()) {
                List<Stadium> courts = stadiumRepository.findCourtsByIds(request.getCourtIds());
                for (Stadium court : courts) {
                    if (court.getNodeType() != StadiumNodeType.COURT) {
                        throw new BadRequestException("ID " + court.getStadiumId() + " không phải là Sân lẻ (COURT)");
                    }
                    if (court.getComplex() == null || !court.getComplex().getComplexId().equals(complexId)) {
                        throw new BadRequestException("Sân lẻ ID " + court.getStadiumId() + " không thuộc Tổ hợp " + complexId);
                    }
                    targetCourts.add(court);
                }
            }

            // Gom nhóm từ facilityIds
            if (request.getFacilityIds() != null && !request.getFacilityIds().isEmpty()) {
                for (Integer facId : request.getFacilityIds()) {
                    Stadium facility = stadiumRepository.findById(facId)
                            .orElseThrow(() -> new ResourceNotFoundException("Khu vực ID " + facId + " không tồn tại"));
                    if (facility.getNodeType() != StadiumNodeType.FACILITY) {
                        throw new BadRequestException("ID " + facId + " không phải là Khu vực (FACILITY)");
                    }
                    if (facility.getComplex() == null || !facility.getComplex().getComplexId().equals(complexId)) {
                        throw new BadRequestException("Khu vực ID " + facId + " không thuộc Tổ hợp " + complexId);
                    }
                    targetCourts.addAll(stadiumRepository.findCourtsByFacilityId(facId));
                }
            }
        }

        // Loại bỏ trùng lặp nếu trùng courtId giữa danh mục lựa chọn
        Set<Integer> courtIdSet = new HashSet<>();
        List<Stadium> uniqueCourts = new ArrayList<>();
        for (Stadium c : targetCourts) {
            if (courtIdSet.add(c.getStadiumId())) {
                uniqueCourts.add(c);
            }
        }

        if (uniqueCourts.isEmpty()) {
            throw new BadRequestException("Không tìm thấy sân lẻ nào khả dụng để áp dụng");
        }

        // 4. Validate slots đầu vào
        List<CreateTimeSlotRequest> sortedSlots = validateAndSortRequestSlots(request.getSlots());

        // 5. Kiểm tra trùng lặp và lưu trữ
        List<TimeSlot> slotsToSave = new ArrayList<>();
        for (Stadium court : uniqueCourts) {
            Stadium facility = court.getParentStadium();
            LocalTime openTime = court.getOpenTime() != null ? court.getOpenTime() :
                    (facility != null ? facility.getOpenTime() : null);
            LocalTime closeTime = court.getCloseTime() != null ? court.getCloseTime() :
                    (facility != null ? facility.getCloseTime() : null);

            if (openTime == null || closeTime == null) {
                throw new BadRequestException("Sân lẻ hoặc Khu vực cha chưa cấu hình giờ mở/đóng cửa hoạt động");
            }

            for (CreateTimeSlotRequest slotReq : sortedSlots) {
                if (slotReq.getStartTime().isBefore(openTime) || slotReq.getEndTime().isAfter(closeTime)) {
                    throw new BadRequestException(String.format(
                            "Khung giờ %s-%s nằm ngoài giờ hoạt động (%s-%s) của sân lẻ ID: %s",
                            slotReq.getStartTime(), slotReq.getEndTime(),
                            openTime, closeTime, court.getStadiumId()));
                }

                List<TimeSlot> overlapping = timeSlotRepository.findOverlappingSlots(
                        court.getStadiumId(), slotReq.getStartTime(), slotReq.getEndTime());
                if (!overlapping.isEmpty()) {
                    throw new BadRequestException(String.format(
                            "Khung giờ %s-%s bị trùng với lịch hiện tại trên sân lẻ ID: %s (%s)",
                            slotReq.getStartTime(), slotReq.getEndTime(),
                            court.getStadiumId(), court.getStadiumName()));
                }

                TimeSlot timeSlot = timeSlotMapper.toEntity(slotReq);
                timeSlot.setStadium(court);
                timeSlot.setSlotStatus(SlotStatus.AVAILABLE);
                slotsToSave.add(timeSlot);
            }
        }

        List<TimeSlot> saved = timeSlotRepository.saveAll(slotsToSave);
        return saved.stream()
                .map(timeSlotMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void validateFacilityOwnership(Stadium facility, Integer userId) {
        StadiumComplex complex = facility.getComplex();
        if (complex == null || !complex.getOwner().getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền quản lý khung giờ cho khu vực này");
        }
    }

    private List<CreateTimeSlotRequest> validateAndSortRequestSlots(List<CreateTimeSlotRequest> slots) {
        List<CreateTimeSlotRequest> sorted = slots.stream()
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            CreateTimeSlotRequest current = sorted.get(i);
            if (!current.getEndTime().isAfter(current.getStartTime())) {
                throw new BadRequestException(String.format(
                        "Giờ kết thúc (%s) phải sau giờ bắt đầu (%s)",
                        current.getEndTime(), current.getStartTime()));
            }

            if (i < sorted.size() - 1) {
                CreateTimeSlotRequest next = sorted.get(i + 1);
                if (current.getEndTime().isAfter(next.getStartTime())) {
                    throw new BadRequestException(String.format(
                            "Các khung giờ yêu cầu bị chồng lặp chéo: %s-%s và %s-%s",
                            current.getStartTime(), current.getEndTime(),
                            next.getStartTime(), next.getEndTime()));
                }
            }
        }
        return sorted;
    }
}
