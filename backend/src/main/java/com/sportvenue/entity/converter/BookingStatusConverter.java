package com.sportvenue.entity.converter;

import com.sportvenue.entity.enums.BookingStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


public class BookingStatusConverter implements AttributeConverter<BookingStatus, String> {

    @Override
    public String convertToDatabaseColumn(BookingStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case PENDING -> "Pending";
            case CONFIRMED -> "Confirmed";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }

    @Override
    public BookingStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return switch (dbData) {
            case "Pending" -> BookingStatus.PENDING;
            case "Confirmed" -> BookingStatus.CONFIRMED;
            case "Completed" -> BookingStatus.COMPLETED;
            case "Cancelled" -> BookingStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unknown database value: " + dbData);
        };
    }
}
