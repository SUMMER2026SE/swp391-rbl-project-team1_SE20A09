package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateTimeSlotRequest;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.mapper.TimeSlotMapper;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final StadiumRepository stadiumRepository;
    private final TimeSlotMapper timeSlotMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getSlotsByStadiumId(Integer stadiumId) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found"));
        return stadium.getTimeSlots().stream()
                .map(timeSlotMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TimeSlotResponse createSlot(Integer stadiumId, CreateTimeSlotRequest request, Integer userId) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found"));

        validateOwnership(stadium, userId);
        validateSlot(stadium, request);

        TimeSlot timeSlot = timeSlotMapper.toEntity(request);
        timeSlot.setStadium(stadium);
        timeSlot.setSlotStatus(SlotStatus.AVAILABLE);

        return timeSlotMapper.toResponse(timeSlotRepository.save(timeSlot));
    }

    @Override
    @Transactional
    public List<TimeSlotResponse> bulkCreateSlots(Integer stadiumId, List<CreateTimeSlotRequest> requests, Integer userId) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found"));

        validateOwnership(stadium, userId);

        // Sort requests by start time to check internal overlaps easily
        List<CreateTimeSlotRequest> sortedReqs = requests.stream()
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .toList();

        for (int i = 0; i < sortedReqs.size(); i++) {
            CreateTimeSlotRequest current = sortedReqs.get(i);
            validateSlot(stadium, current);
            
            // Check overlap with next slot in the bulk request list
            if (i < sortedReqs.size() - 1) {
                CreateTimeSlotRequest next = sortedReqs.get(i + 1);
                if (current.getEndTime().isAfter(next.getStartTime())) {
                    throw new BadRequestException(String.format(
                            "Overlapping slots in request: %s-%s and %s-%s",
                            current.getStartTime(), current.getEndTime(),
                            next.getStartTime(), next.getEndTime()));
                }
            }
        }

        return sortedReqs.stream().map(req -> {
            TimeSlot slot = timeSlotMapper.toEntity(req);
            slot.setStadium(stadium);
            slot.setSlotStatus(SlotStatus.AVAILABLE);
            return timeSlotMapper.toResponse(timeSlotRepository.save(slot));
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSlot(Integer slotId, Integer userId) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found"));
        
        validateOwnership(slot.getStadium(), userId);

        if (slot.getSlotStatus() == SlotStatus.BOOKED) {
            throw new BadRequestException("Cannot delete a booked slot. Please cancel the booking first.");
        }

        timeSlotRepository.delete(slot);
    }

    @Override
    @Transactional
    public TimeSlotResponse toggleSlotStatus(Integer slotId, Integer userId) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found"));

        validateOwnership(slot.getStadium(), userId);

        if (slot.getSlotStatus() == SlotStatus.BOOKED) {
            throw new BadRequestException("Cannot modify a booked slot");
        }

        slot.setSlotStatus(slot.getSlotStatus() == SlotStatus.AVAILABLE ? SlotStatus.MAINTENANCE : SlotStatus.AVAILABLE);
        return timeSlotMapper.toResponse(timeSlotRepository.save(slot));
    }

    private void validateOwnership(Stadium stadium, Integer userId) {
        if (!stadium.getOwner().getUser().getUserId().equals(userId)) {
            throw new BadRequestException("You do not have permission to manage slots for this stadium");
        }
    }

    private void validateSlot(Stadium stadium, CreateTimeSlotRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException(String.format(
                    "End time (%s) must be after start time (%s)", 
                    request.getEndTime(), request.getStartTime()));
        }

        // Validate operating hours
        if (request.getStartTime().isBefore(stadium.getOpenTime()) || 
            request.getEndTime().isAfter(stadium.getCloseTime())) {
            throw new BadRequestException(String.format(
                    "Slot %s-%s is outside stadium operating hours (%s-%s)",
                    request.getStartTime(), request.getEndTime(),
                    stadium.getOpenTime(), stadium.getCloseTime()));
        }

        List<TimeSlot> existingSlots = timeSlotRepository.findOverlappingSlots(
                stadium.getStadiumId(), request.getStartTime(), request.getEndTime());
        
        if (!existingSlots.isEmpty()) {
            TimeSlot conflict = existingSlots.get(0);
            throw new BadRequestException(String.format(
                    "Time slot %s-%s overlaps with existing slot %s-%s",
                    request.getStartTime(), request.getEndTime(),
                    conflict.getStartTime(), conflict.getEndTime()));
        }
    }
}
