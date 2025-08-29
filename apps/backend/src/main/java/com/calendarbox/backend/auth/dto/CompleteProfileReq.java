package com.calendarbox.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompleteProfileReq(
        @NotBlank(message="{member.name.notblank}")
        @Pattern(regexp="^[가-힣a-zA-Z ]{2,30}$", message="이름은 2~30자의 한글/영문만 가능합니다.")
        String name,

        @NotBlank(message="{member.phone.required}")
        @Pattern(regexp="^01[016789]-\\d{3,4}-\\d{4}$", message="{member.phone.pattern}")
        String phoneNumber
) {}

