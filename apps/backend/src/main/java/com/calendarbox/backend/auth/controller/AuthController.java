package com.calendarbox.backend.auth.controller;

import com.calendarbox.backend.auth.dto.*;
import com.calendarbox.backend.auth.service.JwtService;
import com.calendarbox.backend.auth.service.RefreshTokenService;
import com.calendarbox.backend.auth.service.SignupService;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.kakao.domain.KakaoAccount;
import com.calendarbox.backend.kakao.repository.KakaoAccountRepository;
import com.calendarbox.backend.kakao.service.KakaoTempStore;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthController {
    private final JwtService jwtService;
    private final SignupService signupService;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;
    private final KakaoTempStore kakaoTempStore;
    private final KakaoAccountRepository kakaoAccountRepository;

    @PostMapping("/signup/complete")
    public ResponseEntity<ApiResponse<LoginSuccessResponse>> completeProfile(
            HttpServletRequest request,
            @Valid @RequestBody CompleteProfileReq req) {
        String signupToken = readCookie(request, "signup_token")
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
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
        return ResponseEntity.ok(ApiResponse.ok("회원가입이 완료되었습니다", new LoginSuccessResponse(accessToken,refreshToken,mr)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginSuccessResponse>> refresh(
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
        return ResponseEntity.ok(ApiResponse.ok("토큰 재발급 성공",new LoginSuccessResponse(newAccess, newRefresh,memberResponse)));
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

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest req) {
        // 1. 쿠키에서 access_token 읽기
        var accessToken = readCookie(req, "access_token")
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_LOGGED_IN));

        // 2. 토큰 검증 → memberId 반환
        Long memberId = jwtService.verifyAccessToken(accessToken);

        // 3. Member 조회 (MemberRepository 사용)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 4. MemberResponse 리턴
        return ResponseEntity.ok(
                new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getPhoneNumber())
        );
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst();
    }
}
