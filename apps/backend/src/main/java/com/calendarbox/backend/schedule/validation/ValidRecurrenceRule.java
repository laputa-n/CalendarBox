package com.calendarbox.backend.schedule.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RecurrenceRuleValidator.class)
public @interface ValidRecurrenceRule {
    String message() default "Invalid recurrence rule";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

