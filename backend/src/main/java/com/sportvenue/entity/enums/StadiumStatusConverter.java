package com.sportvenue.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StadiumStatusConverter implements AttributeConverter<StadiumStatus, String> {

    @Override
    public String convertToDatabaseColumn(StadiumStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case AVAILABLE -> "Available";
            case MAINTENANCE -> "Maintenance";
            case CLOSED -> "Closed";
        };
    }

    @Override
    public StadiumStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return StadiumStatus.valueOf(dbData.toUpperCase());
    }
}
