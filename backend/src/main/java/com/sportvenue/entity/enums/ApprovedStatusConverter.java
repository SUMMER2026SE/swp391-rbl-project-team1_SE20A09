package com.sportvenue.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApprovedStatusConverter implements AttributeConverter<ApprovedStatus, String> {

    @Override
    public String convertToDatabaseColumn(ApprovedStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case PENDING -> "Pending";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
        };
    }

    @Override
    public ApprovedStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // Chuyển đổi "Approved" thành PENDING/APPROVED/REJECTED
        return ApprovedStatus.valueOf(dbData.toUpperCase());
    }
}
