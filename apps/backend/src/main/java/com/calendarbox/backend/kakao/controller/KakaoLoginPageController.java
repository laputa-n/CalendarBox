package com.calendarbox.backend.kakao.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/api/auth/kakao")
public class KakaoLoginPageController {

    @Value("${kakao.client_id}")
    private String client_id;

    @Value("${kakao.redirect_uri}")
    private String redirect_uri;

    @GetMapping("/login")
    public String loginPage() {
        String location = UriComponentsBuilder.fromHttpUrl("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", client_id)
                .queryParam("redirect_uri", redirect_uri) // 자동 인코딩
                // .queryParam("scope", "account_email") // 이메일을 필수로 쓸 거면 스코프 추가(콘솔 설정도 함께)
                .toUriString();
        return "redirect:" + location;
    }
}