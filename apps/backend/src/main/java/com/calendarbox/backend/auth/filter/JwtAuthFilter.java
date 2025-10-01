package com.calendarbox.backend.auth.filter;

import com.calendarbox.backend.auth.dto.UserPrincipal;
import com.calendarbox.backend.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import io.jsonwebtoken.JwtException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = null;

        // 1. Authorization 헤더 우선
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        }

        // 2. 없으면 access_token 쿠키 시도
        if (token == null && req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("access_token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            try {
                Long memberId = jwtService.verifyAccessToken(token);
                UserPrincipal principal = new UserPrincipal(
                        memberId,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.authorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException e) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        chain.doFilter(req, res);
    }

}
