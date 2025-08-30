package com.calendarbox.backend.auth.service;

import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.kakao.domain.KakaoAccount;
import com.calendarbox.backend.kakao.repository.KakaoAccountRepository;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final MemberRepository memberRepository;
    private final KakaoAccountRepository kakaoAccountRepository;

    @Transactional
    public Member createMemberWithKakao(Long kakaoId, String email, String name, String phone) {
        // profileJson / kakaoRefreshToken 아직 안 쓰므로 null로 위임
        return createMemberWithKakao(kakaoId, email, name, phone, null, null);
    }

    // 🔹 기존 6개 인자 버전 (profileJson, kakaoRefreshToken 포함)
    @Transactional
    public Member createMemberWithKakao(Long kakaoId, String email, String name, String phone,
                                        Map<String,Object> profileJson, String kakaoRefreshToken) {
        // 중복 방지 (동시 요청 대비)
        kakaoAccountRepository.findByProviderUserId(kakaoId).ifPresent(a -> {
            throw new BusinessException(ErrorCode.KAKAO_DUPLICATE_LINK);
        });

        String trimName = name.trim().replaceAll("\\p{C}", "");
        Member m = Member.builder()
                .email(email)
                .name(trimName)
                .phoneNumber(phone)
                .build();
        m = memberRepository.save(m);

        KakaoAccount link = KakaoAccount.builder()
                .member(m)
                .providerUserId(kakaoId)
                .refreshToken(kakaoRefreshToken) // 현재는 null
                .profileJson(profileJson)        // 현재는 null
                .build();
        kakaoAccountRepository.save(link);

        return m;
    }
}
