package com.calendarbox.backend.kakao.controller;

import com.calendarbox.backend.auth.dto.MemberResponse;
import com.calendarbox.backend.auth.service.RefreshTokenService;
import com.calendarbox.backend.kakao.service.KakaoService;
import com.calendarbox.backend.kakao.dto.KakaoUserInfoResponseDto;
import com.calendarbox.backend.kakao.service.KakaoTempStore;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.auth.dto.LoginSuccessResponse;
import com.calendarbox.backend.auth.dto.NextActionResponse;
import com.calendarbox.backend.auth.service.JwtService;
import com.calendarbox.backend.kakao.domain.KakaoAccount;
import com.calendarbox.backend.kakao.repository.KakaoAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<?> callback(@RequestParam("code") String code) {
        // 1) access+refresh 획득
        var token = kakaoService.exchangeToken(code);

        // 2) 사용자 정보
        KakaoUserInfoResponseDto info = kakaoService.getUserInfo(token.getAccessToken());
        Long kakaoId = info.getId();
        String email  = info.getKakaoAccount().getEmail();

        log.info("Kakao callback: kakaoId={}, email={}", kakaoId, email); //테스트를 위한 임시 로그

        // 3) 기존 회원이면 즉시 로그인
        var m = kakaoAccountRepository.findByProviderUserId(kakaoId).map(KakaoAccount::getMember);
        if (m.isPresent()) {
            Member member = m.get();
            String accessToken  = jwtService.issueAccessToken(member);
            String refreshToken = jwtService.issueRefreshToken(member);
            refreshTokenService.save(member.getId(), refreshToken);

            MemberResponse mr = new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getPhoneNumber());
            return ResponseEntity.ok(new LoginSuccessResponse(accessToken, refreshToken, mr));
        }

        // 4) 신규 → 임시 저장
        var profileMap = objectMapper.convertValue(
                info, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>() {});
        kakaoTempStore.save(kakaoId, token.getRefreshToken(), profileMap);

        // 5) 추가 입력 단계 안내
        String signupToken = jwtService.issueTempSignupToken(kakaoId, email);
        return ResponseEntity.ok(new NextActionResponse("COMPLETE_PROFILE", signupToken, email));
    }
}
