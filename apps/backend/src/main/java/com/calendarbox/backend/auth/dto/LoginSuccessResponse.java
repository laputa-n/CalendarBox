package com.calendarbox.backend.auth.dto;

public record LoginSuccessResponse(String accessToken, String refreshToken, MemberResponse member) {
}
