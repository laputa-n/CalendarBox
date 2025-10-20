package com.calendarbox.backend.member.dto.response;

public record MemberSearchItem(
        Long memberId,
        String name,
        String email,
        String phoneNumber
) {
}
