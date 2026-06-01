package com.sportvenue.mapper;

import com.sportvenue.dto.response.AccessoryResponse;
import com.sportvenue.entity.Accessory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccessoryMapper {

    @Mapping(target = "stadiumId", source = "stadium.stadiumId")
    @Mapping(target = "stadiumName", source = "stadium.stadiumName")
    AccessoryResponse toResponse(Accessory accessory);
}
