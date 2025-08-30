package com.calendarbox.backend.auth.controller;

import com.calendarbox.backend.auth.dto.*;
import com.calendarbox.backend.auth.service.JwtService;
import com.calendarbox.backend.auth.service.RefreshTokenService;
import com.calendarbox.backend.auth.service.SignupService;
import com.calendarbox.backend.kakao.service.KakaoTempStore;
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
    private final KakaoTempStore kakaoTempStore;

    @PostMapping("/signup/complete")
    public ResponseEntity<LoginSuccessResponse> completeProfile(
            @RequestHeader("X-Signup-Token") String signupToken,
            @Valid @RequestBody CompleteProfileReq req) {

        SignupClaims claims = jwtService.verifyTempSignupToken(signupToken);

        // ★ 임시 저장 로드
        var temp = kakaoTempStore.load(claims.kakaoId()).orElse(null);

        // ★ 6-인자 버전 호출(임시값이 없으면 null 전달)
        Member member = signupService.createMemberWithKakao(
                claims.kakaoId(),
                claims.email(),
                req.name(),
                req.phoneNumber(),
                temp == null ? null : temp.profileJson(),
                temp == null ? null : temp.refreshToken()
        );

        // 사용 후 제거
        kakaoTempStore.delete(claims.kakaoId());

        String accessToken  = jwtService.issueAccessToken(member);
        String refreshToken = jwtService.issueRefreshToken(member);
        refreshTokenService.save(member.getId(), refreshToken);

        MemberResponse mr = new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getPhoneNumber());
        return ResponseEntity.ok(new LoginSuccessResponse(accessToken, refreshToken, mr));
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
