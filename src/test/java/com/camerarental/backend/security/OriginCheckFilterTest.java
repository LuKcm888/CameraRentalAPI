package com.camerarental.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OriginCheckFilterTest {

    private OriginCheckFilter filter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        // Use same config style as your WebSecurityConfig
        List<String> allowedOrigins = List.of(
                "http://localhost:5173",
                "https://your-frontend.com",
                "http://localhost:3001"
        );

        // These are the ONLY paths the filter enforces origin checks on (see OriginCheckFilter + WebSecurityConfig)
        List<String> protectedPaths = List.of(
                "/api/v1/auth/signin",
                "/api/v1/auth/signup"
        );

        filter = new OriginCheckFilter(allowedOrigins, protectedPaths);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Disallowed origin on protected auth path is blocked (403) and short-circuits the chain")
    void protectedPath_disallowedOrigin_isForbidden() throws ServletException, IOException {
        request.setRequestURI("/api/v1/auth/signin");
        request.setMethod("POST");
        request.addHeader("Origin", "https://evil.com");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("Allowed origin passes for protected auth path")
    void protectedPath_allowedOrigin_passes() throws ServletException, IOException {
        request.setRequestURI("/api/v1/auth/signin");
        request.setMethod("POST");
        request.addHeader("Origin", "http://localhost:5173");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("Missing Origin/Referer passes even on protected auth path (Postman/curl/internal)")
    void protectedPath_missingOriginAndReferer_passes() throws ServletException, IOException {
        request.setRequestURI("/api/v1/auth/signin");
        request.setMethod("POST");
        // no Origin/Referer headers set

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }


    @Test
    @DisplayName("Disallowed origin on unprotected path should be ignored and chain continues")
    void disallowedOrigin_onUnprotectedPath_isIgnored() throws ServletException, IOException {
        request.setRequestURI("/api/v1/projects");
        request.setMethod("POST");
        request.addHeader("Origin", "https://evil.com");

        filter.doFilter(request, response, filterChain);

        // For unprotected path, we *do* expect the chain to run
        verify(filterChain, times(1)).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200); // default, since we didn't set it
    }
}
