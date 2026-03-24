package com.camerarental.backend.security.jwt;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Invalidates JWT tokens on logout by tracking their {@code jti} (JWT ID)
 * until the token's natural expiry.
 *
 * <h3>Why a blacklist?</h3>
 * JWTs are stateless -- the server has no way to revoke one after it has
 * been issued. When a user logs out, we record the token's {@code jti} in
 * this blacklist so that {@link AuthTokenFilter} can reject it on
 * subsequent requests even though the signature is still valid.
 *
 * <h3>Storage strategy</h3>
 * <ul>
 *   <li><b>Redis (preferred)</b> -- entries are stored under
 *       {@code jwt-blacklist:<jti>} with a TTL equal to
 *       {@code spring.app.jwtExpiration} so they auto-expire alongside
 *       the token. This works across multiple application instances.</li>
 *   <li><b>In-memory fallback</b> -- a {@link ConcurrentHashMap} keyed by
 *       {@code jti} with a timestamp-based expiry. Used when Redis is
 *       unavailable or not configured. Entries are lazily evicted on
 *       read. Note: this does <em>not</em> survive restarts and is
 *       local to a single instance.</li>
 * </ul>
 *
 * <h3>Failover behaviour</h3>
 * If a Redis operation fails at runtime the service flips
 * {@code redisAvailable} to {@code false} and silently falls back to
 * the in-memory map for all subsequent calls. This avoids cascading
 * failures but means tokens blacklisted in Redis before the failure
 * will not be visible to the in-memory store.
 */
@Slf4j
@Component
public class JwtBlacklistService {

    private final long jwtExpirationMs;

    /** Local fallback store: jti -> absolute expiry timestamp (epoch ms). */
    private final Map<String, Long> localBlacklist = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedissonClient redissonClient;

    private boolean redisAvailable = false;

    public JwtBlacklistService(@Value("${spring.app.jwtExpiration}") long jwtExpirationMs) {
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /** Probes Redis connectivity on startup and sets the preferred backend. */
    @PostConstruct
    public void init() {
        if (redissonClient != null) {
            try {
                redissonClient.getKeys().count(); // connectivity check
                redisAvailable = true;
                log.info("JWT blacklist initialized with Redis backend");
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis unavailable for JWT blacklist, using in-memory fallback: {}", e.getMessage());
            }
        } else {
            log.info("RedissonClient not configured, using in-memory JWT blacklist");
        }
    }

    /**
     * Records a token's {@code jti} so that future calls to
     * {@link #isBlacklisted(String)} return {@code true} until the
     * token's natural expiry. No-op if {@code jti} is blank.
     */
    public void blacklist(String jti) {
        if (!StringUtils.hasText(jti)) {
            return;
        }

        if (redisAvailable) {
            try {
                RBucket<Boolean> bucket = redissonClient.getBucket(redisKey(jti));
                bucket.set(Boolean.TRUE, jwtExpirationMs, TimeUnit.MILLISECONDS);
                return;
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis write failed for JWT blacklist, falling back to memory: {}", e.getMessage());
            }
        }

        localBlacklist.put(jti, System.currentTimeMillis() + jwtExpirationMs);
    }

    /**
     * Returns {@code true} if the given {@code jti} has been blacklisted
     * and has not yet expired. Lazily evicts stale entries from the
     * in-memory map on each check.
     */
    public boolean isBlacklisted(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }

        if (redisAvailable) {
            try {
                RBucket<Boolean> bucket = redissonClient.getBucket(redisKey(jti));
                Boolean value = bucket.get();
                return Boolean.TRUE.equals(value);
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis read failed for JWT blacklist, falling back to memory: {}", e.getMessage());
            }
        }

        Long expiresAt = localBlacklist.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= System.currentTimeMillis()) {
            localBlacklist.remove(jti);
            return false;
        }
        return true;
    }

    private String redisKey(String jti) {
        return "jwt-blacklist:" + jti;
    }
}
