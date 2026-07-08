package com.sportvenue.mapper;

import com.sportvenue.dto.response.MaintenanceScheduleResponse;
import com.sportvenue.entity.MaintenanceSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MaintenanceScheduleMapper {

    @Mapping(target = "stadiumId", source = "stadium.stadiumId")
    @Mapping(target = "complexId", source = "complex.complexId")
    @Mapping(target = "indefinite", ignore = true)
    @Mapping(target = "active", ignore = true)
    MaintenanceScheduleResponse toResponse(MaintenanceSchedule schedule);
}
