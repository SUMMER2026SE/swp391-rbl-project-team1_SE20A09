package com.sportvenue.mapper;

import com.sportvenue.dto.response.StadiumSearchResponse;
import com.sportvenue.entity.Amenity;
import com.sportvenue.entity.Stadium;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface StadiumMapper {

    @Mapping(target = "sportType", source = "sportType.sportName")
    @Mapping(target = "amenities", source = "amenities", qualifiedByName = "mapAmenities")
    StadiumSearchResponse toStadiumSearchResponse(Stadium stadium);

    @Named("mapAmenities")
    default List<String> mapAmenities(List<Amenity> amenities) {
        if (amenities == null) {
            return null;
        }
        return amenities.stream()
                .map(Amenity::getName)
                .collect(Collectors.toList());
    }
}
