package com.calendarbox.backend.kakao.controller;

import com.calendarbox.backend.auth.service.RefreshTokenService;
import com.calendarbox.backend.auth.util.AuthCookieUtil;
import com.calendarbox.backend.kakao.service.KakaoService;
import com.calendarbox.backend.kakao.service.KakaoTempStore;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.auth.dto.NextActionResponse;
import com.calendarbox.backend.auth.service.JwtService;
import com.calendarbox.backend.kakao.domain.KakaoAccount;
import com.calendarbox.backend.kakao.repository.KakaoAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth/kakao")
public class KakaoLoginController {

    private final KakaoService kakaoService;
    private final JwtService jwtService;
    private final KakaoAccountRepository kakaoAccountRepository;
    private final RefreshTokenService refreshTokenService;
    private final KakaoTempStore kakaoTempStore;
    private final ObjectMapper objectMapper;

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam("code") String code,HttpServletRequest req, HttpServletResponse resp) {
        var token = kakaoService.exchangeToken(code);
        var info  = kakaoService.getUserInfo(token.getAccessToken());
        Long kakaoId = info.getId();
        String email  = info.getKakaoAccount().getEmail();

        var m = kakaoAccountRepository.findByProviderUserId(kakaoId).map(KakaoAccount::getMember);
        boolean local = AuthCookieUtil.isLocal(req);

        if (m.isPresent()) {
            Member member = m.get();
            String accessToken  = jwtService.issueAccessToken(member);
            String refreshToken = jwtService.issueRefreshToken(member);
            refreshTokenService.save(member.getId(), refreshToken);

            resp.addHeader("Set-Cookie", AuthCookieUtil.buildAccessCookie(accessToken, local).toString());
            resp.addHeader("Set-Cookie", AuthCookieUtil.buildRefreshCookie(refreshToken, local).toString());


            String redirect = local ? "http://localhost:3000/login/success" : "/login/success";
            return ResponseEntity.status(302)
                    .header("Location", redirect)
                    .build();
        }

        // 신규
        kakaoTempStore.save(kakaoId, token.getRefreshToken(),
                objectMapper.convertValue(info, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>() {}));

        String signupToken = jwtService.issueTempSignupToken(kakaoId, email);

        resp.addHeader("Set-Cookie", AuthCookieUtil.buildSignupCookie(signupToken, local).toString());

        String redirect = local ? "http://localhost:3000/signup/complete" : "/signup/complete";
        return ResponseEntity.status(302)
                .header("Location", redirect)
                .build();
    }

    @GetMapping("/next")
    public ResponseEntity<?> next(HttpServletRequest req) {
        // 1. 쿠키에서 signup_token 읽기
        var signupToken = readCookie(req, "signup_token").orElseThrow();

        // 2. jwtService로 토큰 유효성 검증 및 claims(토큰 내용물) 꺼내기
        var claims = jwtService.verifyTempSignupToken(signupToken);

        // 3. 응답으로 NextActionResponse 리턴
        return ResponseEntity.ok(
                new NextActionResponse("COMPLETE_PROFILE", null, claims.email())
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
