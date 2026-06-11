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
    @Mapping(target = "capacity", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "timeSlots", ignore = true)
    @Mapping(target = "accessories", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    @Mapping(target = "amenities", ignore = true)
    Stadium toEntity(CreateStadiumRequest request);

    @Mapping(target = "sportName", source = "sportType.sportName")
    @Mapping(target = "imageUrls", expression = "java(stadium.getImages() == null ? java.util.Collections.emptyList() : stadium.getImages().stream().map(img -> img.getImageUrl()).toList())")
    @Mapping(target = "firstImageUrl", expression = "java(stadium.getImages() == null || stadium.getImages().isEmpty() ? null : stadium.getImages().stream().findFirst().map(img -> img.getImageUrl()).orElse(null))")
    @Mapping(target = "distanceInKm", ignore = true)
    StadiumResponse toResponse(Stadium stadium);
}
