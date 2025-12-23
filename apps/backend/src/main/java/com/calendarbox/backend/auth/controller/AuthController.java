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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static com.calendarbox.backend.auth.util.AuthCookieUtil.*;

@Tag(name = "Auth", description = "인증 및 권한")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtService jwtService;
    private final SignupService signupService;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;
    private final KakaoTempStore kakaoTempStore;
    private final KakaoAccountRepository kakaoAccountRepository;

    @Operation(
            summary = "회원 정보 추가 작성",
            description = "회원 가입을 위한 정보를 추가 작성합니다."
    )
    @PostMapping("/signup/complete")
    public ResponseEntity<ApiResponse<MemberResponse>> completeProfile(
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @RequestBody CompleteProfileReq req) {

        String signupToken = readCookie(request, "signup_token")
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        SignupClaims claims = jwtService.verifyTempSignupToken(signupToken);

        var temp = kakaoTempStore.load(claims.kakaoId()).orElse(null);

        Member member = signupService.createMemberWithKakao(
                claims.kakaoId(), claims.email(), req.name(), req.phoneNumber(),
                temp == null ? null : temp.profileJson(),
                temp == null ? null : temp.refreshToken()
        );
        kakaoTempStore.delete(claims.kakaoId());

        String accessToken  = jwtService.issueAccessToken(member);
        String refreshToken = jwtService.issueRefreshToken(member);
        refreshTokenService.save(member.getId(), refreshToken);

        boolean local = isLocal(request);
        response.addHeader(HttpHeaders.SET_COOKIE, buildAccessCookie(accessToken, local).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken, local).toString());

        ResponseCookie expiredSignup = ResponseCookie.from("signup_token", "")
                .httpOnly(true)
                .secure(!local)
                .sameSite(local ? "Lax" : "None")
                .path("/")   // 원래 발급된 경로와 맞춰야 함
                .maxAge(0)   // 즉시 만료
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredSignup.toString());

        MemberResponse mr = new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.ok("회원가입이 완료되었습니다", mr));
    }

    @Operation(
            summary = "토큰 재발급",
            description = "access / refresh 토큰을 재발급 받습니다."
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<MemberResponse>> refresh(HttpServletRequest req,
                                                               HttpServletResponse resp) {
        String refreshToken = readCookie(req, "refresh_token")
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_LOGGED_IN));

        RefreshClaims claims = jwtService.verifyRefreshToken(refreshToken);
        Long memberId = claims.memberId();

        if (!refreshTokenService.matches(memberId, refreshToken)) {
            refreshTokenService.delete(memberId);
            return ResponseEntity.status(401).build();
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String newAccess  = jwtService.issueAccessToken(member);
        String newRefresh = jwtService.issueRefreshToken(member); // 회전 권장
        refreshTokenService.replace(memberId, newRefresh);

        boolean local = isLocal(req);
        resp.addHeader(HttpHeaders.SET_COOKIE, buildAccessCookie(newAccess, local).toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(newRefresh, local).toString());

        MemberResponse mr = new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.ok("토큰 재발급 성공", mr));
    }

    @Operation(
            summary = "로그 아웃",
            description = "로그 아웃을 합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal(expression = "id") Long memberId,
                                       HttpServletRequest req,
                                       HttpServletResponse resp) {
        if (memberId != null) {
            refreshTokenService.delete(memberId);
        }
        boolean local = isLocal(req);
        resp.addHeader(HttpHeaders.SET_COOKIE, deleteCookie("access_token", local, "/").toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, deleteCookie("refresh_token", local, "/api/auth").toString());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "내 정보 확인",
            description = "내 정보를 확인합니다."
    )
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
