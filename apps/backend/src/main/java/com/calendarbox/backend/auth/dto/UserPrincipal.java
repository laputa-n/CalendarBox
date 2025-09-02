package com.calendarbox.backend.auth.dto;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public record UserPrincipal(Long id, Collection<? extends GrantedAuthority> authorities) {
}
