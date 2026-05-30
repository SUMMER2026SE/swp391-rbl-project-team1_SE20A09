package com.sportvenue.mapper;

import com.sportvenue.dto.request.CreateStadiumRequest;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.entity.Stadium;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StadiumMapper {

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "sportType", ignore = true)
    @Mapping(target = "stadiumId", ignore = true)
    @Mapping(target = "stadiumStatus", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "timeSlots", ignore = true)
    @Mapping(target = "accessories", ignore = true)
    Stadium toEntity(CreateStadiumRequest request);

    @Mapping(target = "sportName", source = "sportType.sportName")
    StadiumResponse toResponse(Stadium stadium);
}
