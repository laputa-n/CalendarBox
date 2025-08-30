package com.calendarbox.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompleteProfileReq(
        @NotBlank(message="{member.name.notblank}")
        @Pattern(
                regexp = "^[\\p{L} ][\\p{L} ]{1,29}$", // 총 2~30자, 모든 ‘문자’와 공백 허용
                message = "{member.name.pattern}"
        )
        String name,

        @NotBlank(message="{member.phone.required}")
        @Pattern(
                regexp="^01[016789]-\\d{3,4}-\\d{4}$",
                message="{member.phone.pattern}"
        )
        String phoneNumber
) {}

