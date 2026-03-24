package com.camerarental.backend.security.jwt;



import com.camerarental.backend.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * Utility class for JWT generation, parsing, and validation.
 *
 * <p>Tokens are signed with an HMAC-SHA key derived from the
 * Base64-encoded secret in {@code spring.app.jwtSecret}. Each token
 * carries the username as its {@code subject} and a random
 * {@code jti} (JWT ID) that enables per-token blacklisting on
 * logout via {@link JwtBlacklistService}.</p>
 */
@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpiration}")
    private int jwtExpirationMs;

    /**
     * Generate a JWT for the given user.
     * This is now used for Authorization: Bearer, not cookies.
     */
    public String generateJwtToken(UserDetailsImpl userPrincipal) {
        return generateTokenFromUserName(userPrincipal.getUsername());
    }

    // Generate token from the username
    public String generateTokenFromUserName(String username) {
        return Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date((new Date().getTime() + jwtExpirationMs)))
                .signWith(key())
                .compact();
    }

    // Get username from JWT token
    public String getUserNameFromJWTToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Extract JWT ID (jti) from the token
    public String getJtiFromToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getId();
    }

    // Generate signing key
    public Key key() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret)
        );
    }

    // Validate JWT token
    public boolean validateJwtToken(String authToken) {
        try {
            logger.debug("Attempting to validate JWT token");
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            logger.error("JWT signature invalid: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
