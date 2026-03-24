package com.camerarental.backend.security.jwt;


import com.camerarental.backend.security.services.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class AuthTokenFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal - sets authentication when JWT is valid")
    void doFilterInternal_validToken_setsAuthentication() throws Exception {
        // given
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtBlacklistService jwtBlacklistService = mock(JwtBlacklistService.class);
        AuthTokenFilter filter = new AuthTokenFilter(jwtUtils, userDetailsService, jwtBlacklistService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/projects");
        // New behavior: token comes from Authorization header, not cookies
        request.addHeader("Authorization", "Bearer test-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        given(jwtUtils.validateJwtToken("test-token")).willReturn(true);
        given(jwtUtils.getUserNameFromJWTToken("test-token")).willReturn("user1");

        UserDetailsImpl details = new UserDetailsImpl(
                UUID.randomUUID(),
                "user1",
                "user1@example.com",
                "encoded",
                List.of(() -> "ROLE_CUSTOMER")
        );
        given(userDetailsService.loadUserByUsername("user1")).willReturn(details);

        // when
        filter.doFilterInternal(request, response, chain);

        // then
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(details);
        assertThat(authentication.getAuthorities()).hasSize(1);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - does nothing when token is missing")
    void doFilterInternal_noToken_doesNothing() throws Exception {
        // given
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtBlacklistService jwtBlacklistService = mock(JwtBlacklistService.class);
        AuthTokenFilter filter = new AuthTokenFilter(jwtUtils, userDetailsService, jwtBlacklistService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/projects");
        // No Authorization header → parseJwt will return null
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtils, never()).validateJwtToken(anyString());
    }

    @Test
    @DisplayName("doFilterInternal - blocks when token jti is blacklisted")
    void doFilterInternal_blacklistedToken_returns401AndSkipsChain() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtBlacklistService jwtBlacklistService = mock(JwtBlacklistService.class);
        AuthTokenFilter filter = new AuthTokenFilter(jwtUtils, userDetailsService, jwtBlacklistService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/projects");
        request.addHeader("Authorization", "Bearer test-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        given(jwtUtils.validateJwtToken("test-token")).willReturn(true);
        given(jwtUtils.getJtiFromToken("test-token")).willReturn("jti-123");
        given(jwtBlacklistService.isBlacklisted("jti-123")).willReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(any(), any());
    }
}
