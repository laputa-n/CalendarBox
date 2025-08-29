package com.calendarbox.backend.kakao.controller;

import com.calendarbox.backend.auth.dto.MemberResponse;
import com.calendarbox.backend.auth.service.RefreshTokenService;
import com.calendarbox.backend.kakao.service.KakaoService;
import com.calendarbox.backend.kakao.dto.KakaoUserInfoResponseDto;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.auth.dto.LoginSuccessResponse;
import com.calendarbox.backend.auth.dto.NextActionResponse;
import com.calendarbox.backend.auth.service.JwtService;
import com.calendarbox.backend.kakao.domain.KakaoAccount;
import com.calendarbox.backend.kakao.repository.KakaoAccountRepository;
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
@RequestMapping("/auth/kakao")
public class KakaoLoginController {

    private final KakaoService kakaoService;
    private final JwtService jwtService;
    private final KakaoAccountRepository kakaoAccountRepository;
    private final RefreshTokenService refreshTokenService;

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam("code") String code) {
        String kakaoAccessToken = kakaoService.getAccessTokenFromKakao(code);
        KakaoUserInfoResponseDto info = kakaoService.getUserInfo(kakaoAccessToken);

        Long kakaoId = info.getId();
        String email = info.getKakaoAccount().getEmail();

        // 1) 이미 가입된 경우 -> 로그인
        Optional<Member>  m = kakaoAccountRepository
                .findByProviderUserId(kakaoId)
                .map(KakaoAccount::getMember);

        if (m.isPresent()) {
            Member member = m.get();

            String accessToken = jwtService.issueAccessToken(member);
            String refreshToken = jwtService.issueRefreshToken(member);

            refreshTokenService.save(member.getId(), refreshToken);

            MemberResponse memberResponse = new MemberResponse(member.getId(), member.getName(), member.getEmail(),member.getPhoneNumber());
            return ResponseEntity.ok(new LoginSuccessResponse(accessToken,refreshToken,memberResponse));
        }

        // 2) 미가입 -> 추가 입력으로
        String signupToken = jwtService.issueTempSignupToken(kakaoId, email);
        return ResponseEntity.ok(new NextActionResponse("COMPLETE_PROFILE", signupToken, email));
    }
}
