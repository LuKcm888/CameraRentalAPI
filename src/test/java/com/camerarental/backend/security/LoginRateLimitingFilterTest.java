package com.camerarental.backend.security;


import com.camerarental.backend.config.ApiPaths;
import com.camerarental.backend.security.filters.LoginRateLimitingFilter;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.*;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginRateLimitingFilterTest {

    private LoginRateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        // new instance per test so buckets map is clean
        filter = new LoginRateLimitingFilter();
    }

    @Test
    @DisplayName("Non-signin requests are not rate-limited")
    void nonSigninRequests_areNotFiltered() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", ApiPaths.AUTH + "/signup");
        request.setRemoteAddr("10.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("First 5 login attempts for an IP are allowed")
    void underThreshold_allowsRequests() throws ServletException, IOException {
        String ip = "10.0.0.2";

        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", ApiPaths.AUTH + "/signin");
            request.setRemoteAddr(ip);

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus())
                    .as("Request " + i + " should be allowed")
                    .isEqualTo(200);
        }
    }

    @Test
    @DisplayName("6th login attempt within window is blocked with 429")
    void overThreshold_blocksRequest() throws ServletException, IOException {
        String ip = "10.0.0.3";

        // Consume the 5 allowed attempts
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", ApiPaths.AUTH + "/signin");
            request.setRemoteAddr(ip);

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus())
                    .as("Request " + i + " should be allowed")
                    .isEqualTo(200);
        }

        // 6th attempt → should be blocked
        MockHttpServletRequest blockedRequest =
                new MockHttpServletRequest("POST", ApiPaths.AUTH + "/signin");
        blockedRequest.setRemoteAddr(ip);

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        filter.doFilter(blockedRequest, blockedResponse, blockedChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getContentType()).isEqualTo("application/json");
        assertThat(blockedResponse.getContentAsString())
                .contains("Too many login attempts");
    }

    @Test
    @DisplayName("With Redis available, 6th request is blocked via Redis backend")
    void redisBackend_blocksOnSixthRequest() throws ServletException, IOException {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RKeys rKeys = mock(RKeys.class);
        RRateLimiter rateLimiter = mock(RRateLimiter.class);

        when(redissonClient.getKeys()).thenReturn(rKeys);
        when(rKeys.count()).thenReturn(1L);
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(RateType.class), anyLong(), anyLong(), any(RateIntervalUnit.class))).thenReturn(true);
        when(rateLimiter.tryAcquire(anyLong())).thenReturn(true, true, true, true, true, false);

        ReflectionTestUtils.setField(filter, "redissonClient", redissonClient);
        filter.init();

        String ip = "10.0.0.9";

        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", ApiPaths.AUTH + "/signin");
            request.setRemoteAddr(ip);

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus())
                    .as("Request " + i + " should be allowed")
                    .isEqualTo(200);
        }

        MockHttpServletRequest blockedRequest =
                new MockHttpServletRequest("POST", ApiPaths.AUTH + "/signin");
        blockedRequest.setRemoteAddr(ip);

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        filter.doFilter(blockedRequest, blockedResponse, blockedChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getContentAsString())
                .contains("Too many login attempts");

        Boolean redisAvailable = (Boolean) ReflectionTestUtils.getField(filter, "redisAvailable");
        assertThat(redisAvailable).isTrue();

        verify(rateLimiter, times(6)).tryAcquire(1L);
    }

    @Test
    @DisplayName("Redis failure triggers fallback to memory and still blocks on 6th")
    void redisFailure_fallsBackToMemory() throws ServletException, IOException {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RKeys rKeys = mock(RKeys.class);
        RRateLimiter rateLimiter = mock(RRateLimiter.class);

        when(redissonClient.getKeys()).thenReturn(rKeys);
        when(rKeys.count()).thenReturn(1L);
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(RateType.class), anyLong(), anyLong(), any(RateIntervalUnit.class))).thenReturn(true);
        when(rateLimiter.tryAcquire(anyLong())).thenThrow(new RuntimeException("Redis down"));

        ReflectionTestUtils.setField(filter, "redissonClient", redissonClient);
        filter.init();

        String ip = "10.0.0.10";

        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", ApiPaths.AUTH + "/signin");
            request.setRemoteAddr(ip);

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus())
                    .as("Request " + i + " should be allowed via fallback")
                    .isEqualTo(200);
        }

        MockHttpServletRequest blockedRequest =
                new MockHttpServletRequest("POST", ApiPaths.AUTH + "/signin");
        blockedRequest.setRemoteAddr(ip);

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        filter.doFilter(blockedRequest, blockedResponse, blockedChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getContentType()).isEqualTo("application/json");

        Boolean redisAvailable = (Boolean) ReflectionTestUtils.getField(filter, "redisAvailable");
        assertThat(redisAvailable).isFalse();
    }
}
