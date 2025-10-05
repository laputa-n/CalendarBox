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

        String emailToken = toEmailPrefix(raw);
        String phoneToken = (emailToken == null) ? toPhonePrefix(raw): null;

        if (emailToken != null && emailToken.length() < 3) emailToken = null;
        if (phoneToken != null && phoneToken.length() < 3) phoneToken = null;

        if (emailToken == null && phoneToken == null) return Page.empty(pageable);

        return memberRepository.searchByEmailOrPhone(user.getId(),emailToken, phoneToken, pageable);
    }

    private String toEmailPrefix(String s) {
        String t = s.toLowerCase();
        return t.contains("@") || t.matches("^[a-z0-9._%+-]+$") ? t.replaceAll("\\s+", "") : null;
    }

    private String toPhonePrefix(String s) {
        String digits = s.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }
}
