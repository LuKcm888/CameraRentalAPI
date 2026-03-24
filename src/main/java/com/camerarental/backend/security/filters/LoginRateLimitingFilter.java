package com.camerarental.backend.security.filters;

import com.camerarental.backend.config.ApiPaths;
import tools.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LoginRateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Optional - may be null if Redis is not configured
    @Autowired(required = false)
    private RedissonClient redissonClient;

    // In-memory fallback
    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    private boolean redisAvailable = false;

    @PostConstruct
    public void init() {
        if (redissonClient != null) {
            try {
                // Test connection
                redissonClient.getKeys().count();
                redisAvailable = true;
                log.info("Rate limiting initialized with Redis backend");
            } catch (Exception e) {
                log.warn("Redis unavailable, using in-memory rate limiting: {}", e.getMessage());
                redisAvailable = false;
            }
        } else {
            log.info("RedissonClient not configured, using in-memory rate limiting");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !((ApiPaths.AUTH + "/signin").equals(request.getRequestURI()) &&
                "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        boolean allowed;

        if (redisAvailable) {
            allowed = tryAcquireFromRedis(ip);
        } else {
            allowed = tryAcquireFromMemory(ip);
        }

        if (allowed) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("rate_limit_exceeded ip={} path={} backend={}",
                    ip, request.getRequestURI(), redisAvailable ? "redis" : "memory");

            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "message", "Too many login attempts. Please try again later.",
                    "status", false
            )));
        }
    }

    private boolean tryAcquireFromRedis(String ip) {
        try {
            String key = "login-rate:" + ip;
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
            rateLimiter.trySetRate(RateType.OVERALL, 5, 1, RateIntervalUnit.MINUTES);
            return rateLimiter.tryAcquire(1);
        } catch (Exception e) {
            log.warn("Redis error, falling back to memory: {}", e.getMessage());
            redisAvailable = false;  // Switch to fallback
            return tryAcquireFromMemory(ip);
        }
    }

    private boolean tryAcquireFromMemory(String ip) {
        Bucket bucket = localBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                        .build()
        );
        return bucket.tryConsume(1);
    }
}