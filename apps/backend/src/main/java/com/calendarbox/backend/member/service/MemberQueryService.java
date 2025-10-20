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
        Member user = memberRepository.findById(userId).orElseThrow(() ->new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if(query == null) return Page.empty(pageable);
        String raw = query.trim();
        if(raw.isEmpty()) return Page.empty(pageable);

        String emailToken = toEmailPrefixOrNull(raw);
        String phoneToken = toPhonePrefixOrNull(raw);
        String nameToken = toNamePrefixOrNull(raw);

        if (emailToken != null) emailToken = emailToken.toLowerCase(Locale.ROOT);
        if (nameToken  != null) nameToken  = nameToken.toLowerCase(Locale.ROOT);

        if (emailToken != null && emailToken.length() < 3) emailToken = null;
        if (phoneToken != null && phoneToken.length() < 3) phoneToken = null;
        if (nameToken  != null && nameToken.length()  < 3) nameToken  = null;

        if (emailToken == null && phoneToken == null && nameToken == null) {
            return Page.empty(pageable);
        }

        return memberRepository.searchByEmailOrPhoneOrName(
                user.getId(), emailToken, phoneToken, nameToken, pageable
        );
    }

    private String toEmailPrefixOrNull(String raw) {
        String s = raw.trim();
        if (!s.contains("@")) return null;
        return s;
    }

    private String toPhonePrefixOrNull(String raw) {
        String digits = raw.replaceAll("\\D+", "");
        return digits.isEmpty() ? null : digits;
    }

    private String toNamePrefixOrNull(String raw) {
        String s = raw.trim().replaceAll("\\s+", " ");
        if (s.isEmpty() || s.matches("\\d+")) return null;
        return s;
    }
}
