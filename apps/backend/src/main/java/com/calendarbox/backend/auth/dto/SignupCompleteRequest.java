package com.calendarbox.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupCompleteRequest(
        @NotBlank(message="{member.name.notblank}") String name,
        @Pattern(regexp="^01[016789]-\\d{3,4}-\\d{4}$", message="{member.phone.pattern}") String phoneNumber
) {}

