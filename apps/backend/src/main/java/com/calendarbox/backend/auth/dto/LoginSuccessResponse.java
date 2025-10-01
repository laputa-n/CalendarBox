package com.calendarbox.backend.auth.dto;

public record LoginSuccessResponse(String accessToken, MemberResponse member) {
}
