package com.calendarbox.backend.auth.controller;

import com.calendarbox.backend.auth.dto.*;
import com.calendarbox.backend.auth.service.JwtService;
import com.calendarbox.backend.auth.service.RefreshTokenService;
import com.calendarbox.backend.auth.service.SignupService;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final JwtService jwtService;
    private final SignupService signupService;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;

    @PostMapping("/signup/complete")
    public ResponseEntity<LoginSuccessResponse> completeProfile(
            @RequestHeader("X-Signup-Token") String signupToken,
            @Valid @RequestBody CompleteProfileReq req) {   // 👈 @Valid 추가

        SignupClaims claims = jwtService.verifyTempSignupToken(signupToken);
        Member member = signupService.createMemberWithKakao(
                claims.kakaoId(), claims.email(), req.name(), req.phoneNumber());
        String accessToken = jwtService.issueAccessToken(member);
        String refreshToken = jwtService.issueRefreshToken(member);

        refreshTokenService.save(member.getId(),refreshToken);
        MemberResponse memberResponse = new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getPhoneNumber());
        return ResponseEntity.ok(new LoginSuccessResponse(accessToken,refreshToken,memberResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginSuccessResponse> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {

        RefreshClaims claims = jwtService.verifyRefreshToken(refreshToken);
        Long memberId = claims.memberId();

        if (!refreshTokenService.matches(memberId, refreshToken)) {
            refreshTokenService.delete(memberId);
            return ResponseEntity.status(401).build();
        }

        Member member = memberRepository.findById(memberId).orElseThrow();
        String newAccess  = jwtService.issueAccessToken(member);
        String newRefresh = jwtService.issueRefreshToken(member);
        refreshTokenService.replace(memberId, newRefresh);

        MemberResponse memberResponse = new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getPhoneNumber());
        return ResponseEntity.ok(new LoginSuccessResponse(newAccess, newRefresh,memberResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value="Authorization", required=false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                Long memberId = jwtService.verifyAccessToken(auth.substring(7));
                refreshTokenService.delete(memberId);
            } catch (Exception ignore) {}
        }
        return ResponseEntity.noContent().build();
    }
}
