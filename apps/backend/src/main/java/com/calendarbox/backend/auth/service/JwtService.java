package com.calendarbox.backend.auth.service;

import com.calendarbox.backend.auth.dto.RefreshClaims;
import com.calendarbox.backend.auth.dto.SignupClaims;
import com.calendarbox.backend.member.domain.Member;

public interface JwtService {
    String issueTempSignupToken(Long kakaoId, String email);
    SignupClaims verifyTempSignupToken(String token);

    // 로그인용 액세스 토큰 발급
    String issueAccessToken(Member member);
    Long verifyAccessToken(String token);

    // 리프레쉬 토큰
    String issueRefreshToken(Member member);
    RefreshClaims verifyRefreshToken(String token);
}
