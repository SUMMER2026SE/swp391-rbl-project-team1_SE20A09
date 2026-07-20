package com.sportvenue.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumConstraintValidator implements ConstraintValidator<EnumConstraint, String> {
    private Set<String> acceptedValues;
    private boolean nullable;

    @Override
    public void initialize(EnumConstraint constraintAnnotation) {
        acceptedValues = Arrays.stream(constraintAnnotation.value().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
        nullable = constraintAnnotation.nullable();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return nullable;
        }
        return acceptedValues.contains(value.trim().toUpperCase());
    }
}
