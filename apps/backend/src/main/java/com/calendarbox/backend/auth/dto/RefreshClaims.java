package com.calendarbox.backend.auth.dto;

public record RefreshClaims(Long memberId, String jti) {
}
