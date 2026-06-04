package com.sportvenue.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateStadiumRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validRequestHasNoViolations() {
        Set<ConstraintViolation<CreateStadiumRequest>> violations = validator.validate(validRequest());

        assertTrue(violations.isEmpty());
    }

    @Test
    void rejectsEmptyImages() {
        CreateStadiumRequest request = validRequest();
        request.setImageUrls(List.of());

        Set<ConstraintViolation<CreateStadiumRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "imageUrls"));
    }

    @Test
    void rejectsCloseTimeBeforeOpenTime() {
        CreateStadiumRequest request = validRequest();
        request.setOpenTime(LocalTime.of(22, 0));
        request.setCloseTime(LocalTime.of(6, 0));

        Set<ConstraintViolation<CreateStadiumRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "closeTimeAfterOpenTime"));
    }

    @Test
    void rejectsOverlongDescription() {
        CreateStadiumRequest request = validRequest();
        request.setDescription("x".repeat(2001));

        Set<ConstraintViolation<CreateStadiumRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "description"));
    }

    private static CreateStadiumRequest validRequest() {
        return CreateStadiumRequest.builder()
                .stadiumName("Stadium A")
                .address("123 Main Street")
                .sportTypeId(1)
                .pricePerHour(BigDecimal.valueOf(100000))
                .description("Clean synthetic grass")
                .openTime(LocalTime.of(6, 0))
                .closeTime(LocalTime.of(22, 0))
                .imageUrls(List.of("http://localhost:8080/api/v1/files/stadiums/image.jpg"))
                .build();
    }

    private static boolean hasViolation(Set<ConstraintViolation<CreateStadiumRequest>> violations, String property) {
        return violations.stream()
                .anyMatch(violation -> property.equals(violation.getPropertyPath().toString()));
    }
}
