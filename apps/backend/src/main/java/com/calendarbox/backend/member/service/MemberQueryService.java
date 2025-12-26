package com.calendarbox.backend.member.service;

import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.dto.response.MemberSearchItem;
import com.calendarbox.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberQueryService {
    private final MemberRepository memberRepository;

    public Page<MemberSearchItem> search(Long userId, String query, Pageable pageable){
        Member user = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (query == null) return Page.empty(pageable);
        String raw = query.trim();
        if (raw.isEmpty()) return Page.empty(pageable);

        String nameToken  = toNameContainsOrNull(raw);
        String emailToken = toEmailLocalContainsOrNull(raw); // @앞부분만
        String phoneToken = toPhoneContainsOrNull(raw);      // 숫자만

        // 최소 길이(원하는 기준으로 조절)
        if (nameToken  != null && nameToken.length()  < 3) nameToken  = null;
        if (emailToken != null && emailToken.length() < 3) emailToken = null;
        if (phoneToken != null && phoneToken.length() < 3) phoneToken = null;

        if (nameToken == null && emailToken == null && phoneToken == null) {
            return Page.empty(pageable);
        }

        // LIKE 패턴으로 변환
        if (nameToken  != null) nameToken  = "%" + nameToken.toLowerCase(Locale.ROOT) + "%";
        if (emailToken != null) emailToken = "%" + emailToken.toLowerCase(Locale.ROOT) + "%";
        if (phoneToken != null) phoneToken = "%" + phoneToken + "%";

        return memberRepository.searchByEmailLocalOrPhoneOrNameContains(
                user.getId(), emailToken, phoneToken, nameToken, pageable
        );
    }

    private String toNameContainsOrNull(String raw) {
        String s = raw.trim().replaceAll("\\s+", " ");
        if (s.isEmpty() || s.matches("\\d+")) return null; // 숫자만이면 이름 후보 제외
        return s;
    }

    // 사용자가 "ma9611@naver.com" 입력하면 ma9611만 사용
// 사용자가 "ma9611" 입력해도 이메일 local-part로 검색 허용
    private String toEmailLocalContainsOrNull(String raw) {
        String s = raw.trim();
        if (s.isEmpty()) return null;
        if (s.matches("\\d+")) return null; // 숫자만이면 이메일 후보 제외
        int at = s.indexOf('@');
        if (at >= 0) s = s.substring(0, at);
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private String toPhoneContainsOrNull(String raw) {
        String digits = raw.replaceAll("\\D+", "");
        return digits.isEmpty() ? null : digits;
    }

}
