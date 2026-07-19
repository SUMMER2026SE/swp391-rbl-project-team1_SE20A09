package com.sportvenue.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = EnumConstraintValidator.class)
public @interface EnumConstraint {
    String message() default "Must be one of the accepted values";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    Class<? extends Enum<?>> value();
    boolean nullable() default true;
}
