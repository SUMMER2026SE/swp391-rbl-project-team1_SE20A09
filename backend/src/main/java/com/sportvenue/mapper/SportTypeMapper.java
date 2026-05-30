package com.sportvenue.mapper;

import com.sportvenue.dto.response.SportTypeResponse;
import com.sportvenue.entity.SportType;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SportTypeMapper {
    SportTypeResponse toResponse(SportType sportType);
    List<SportTypeResponse> toResponseList(List<SportType> sportTypes);
}
