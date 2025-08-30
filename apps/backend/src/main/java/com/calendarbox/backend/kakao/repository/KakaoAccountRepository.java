package com.calendarbox.backend.kakao.repository;

import com.calendarbox.backend.kakao.domain.KakaoAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KakaoAccountRepository extends JpaRepository<KakaoAccount, Long> {
    Optional<KakaoAccount> findByProviderUserId(Long providerUserId);
}
