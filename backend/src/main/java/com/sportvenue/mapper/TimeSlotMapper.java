package com.sportvenue.mapper;

import com.sportvenue.dto.request.CreateTimeSlotRequest;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.TimeSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TimeSlotMapper {
    @Mapping(target = "stadiumId", source = "stadium.stadiumId")
    @Mapping(target = "available", ignore = true)
    TimeSlotResponse toResponse(TimeSlot timeSlot);

    @Mapping(target = "slotId", ignore = true)
    @Mapping(target = "stadium", ignore = true)
    @Mapping(target = "slotStatus", ignore = true)
    TimeSlot toEntity(CreateTimeSlotRequest request);
}
