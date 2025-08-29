package com.calendarbox.backend.auth.service;

import com.calendarbox.backend.kakao.domain.KakaoAccount;
import com.calendarbox.backend.kakao.repository.KakaoAccountRepository;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.global.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final MemberRepository memberRepository;
    private final KakaoAccountRepository kakaoAccountRepository;

    @Transactional
    public Member createMemberWithKakao(Long kakaoId, String email, String name, String phone) {
        // 중복 방지 (동시 요청 대비)
        kakaoAccountRepository.findByProviderUserId(kakaoId).ifPresent(a -> {
            throw new ConflictException("Kakao account already linked");
        });

        Member m = Member.builder()
                        .email(email)
                        .name(name)
                        .phoneNumber(phone)
                        .build();
        m = memberRepository.save(m);

        KakaoAccount link = KakaoAccount.builder()
                .member(m)
                .providerUserId(kakaoId)
                .build();
        kakaoAccountRepository.save(link);
        return m;
    }
}
