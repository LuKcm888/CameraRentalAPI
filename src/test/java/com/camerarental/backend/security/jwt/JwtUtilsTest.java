package com.camerarental.backend.security.jwt;


import com.camerarental.backend.security.services.UserDetailsImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtils Tests")
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private String base64Secret;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new JwtUtils();

        // generate a 256-bit secret for tests
        String rawSecret = "this-is-a-test-secret-key-which-should-be-long-enough";
        base64Secret = Base64.getEncoder().encodeToString(rawSecret.getBytes());

        // match field names in the new JwtUtils
        setField(jwtUtils, "jwtSecret", base64Secret);
        setField(jwtUtils, "jwtExpirationMs", (int) Duration.ofMinutes(5).toMillis());
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = JwtUtils.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // =========================================================================
    // Token Generation Tests
    // =========================================================================

    @Nested
    @DisplayName("Token Generation")
    class TokenGenerationTests {

        @Test
        @DisplayName("generateTokenFromUserName creates a token we can validate and read username from")
        void generateAndValidateToken_fromUsername() {
            String username = "user1";

            String token = jwtUtils.generateTokenFromUserName(username);
            assertThat(token).isNotBlank();

            boolean valid = jwtUtils.validateJwtToken(token);
            assertThat(valid).isTrue();

            String extracted = jwtUtils.getUserNameFromJWTToken(token);
            assertThat(extracted).isEqualTo(username);
        }

        @Test
        @DisplayName("generateJwtToken(UserDetailsImpl) creates a valid token with correct subject")
        void generateJwtToken_fromUserDetails() {
            UserDetailsImpl userDetails = new UserDetailsImpl(
                    UUID.randomUUID(),
                    "user1",
                    "user1@example.com",
                    "encoded",
                    List.of(() -> "ROLE_CUSTOMER")
            );

            String token = jwtUtils.generateJwtToken(userDetails);
            assertThat(token).isNotBlank();

            boolean valid = jwtUtils.validateJwtToken(token);
            assertThat(valid).isTrue();

            String extracted = jwtUtils.getUserNameFromJWTToken(token);
            assertThat(extracted).isEqualTo("user1");
        }

        @Test
        @DisplayName("generated token contains unique JTI (JWT ID)")
        void generatedToken_containsUniqueJti() {
            String token1 = jwtUtils.generateTokenFromUserName("user1");
            String token2 = jwtUtils.generateTokenFromUserName("user1");

            String jti1 = jwtUtils.getJtiFromToken(token1);
            String jti2 = jwtUtils.getJtiFromToken(token2);

            assertThat(jti1).isNotBlank();
            assertThat(jti2).isNotBlank();
            assertThat(jti1).isNotEqualTo(jti2);
        }

        @Test
        @DisplayName("generated tokens for different users have different subjects")
        void generatedTokens_haveDifferentSubjects() {
            String token1 = jwtUtils.generateTokenFromUserName("user1");
            String token2 = jwtUtils.generateTokenFromUserName("user2");

            String username1 = jwtUtils.getUserNameFromJWTToken(token1);
            String username2 = jwtUtils.getUserNameFromJWTToken(token2);

            assertThat(username1).isEqualTo("user1");
            assertThat(username2).isEqualTo("user2");
        }
    }

    // =========================================================================
    // Token Validation - Invalid Token Tests
    // =========================================================================

    @Nested
    @DisplayName("Token Validation - Invalid Tokens")
    class InvalidTokenTests {

        @Test
        @DisplayName("validates that malformed token is rejected")
        void validateJwtToken_rejectsMalformedToken() {
            String malformedToken = "this.is.not.a.valid.jwt";

            boolean valid = jwtUtils.validateJwtToken(malformedToken);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("validates that empty token is rejected")
        void validateJwtToken_rejectsEmptyToken() {
            boolean valid = jwtUtils.validateJwtToken("");

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("validates that null token is rejected")
        void validateJwtToken_rejectsNullToken() {
            boolean valid = jwtUtils.validateJwtToken(null);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("validates that random string is rejected")
        void validateJwtToken_rejectsRandomString() {
            String randomString = UUID.randomUUID().toString();

            boolean valid = jwtUtils.validateJwtToken(randomString);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("validates that token with invalid structure is rejected")
        void validateJwtToken_rejectsInvalidStructure() {
            // A token needs exactly 3 parts separated by dots
            String invalidStructure = "header.payload"; // Missing signature

            boolean valid = jwtUtils.validateJwtToken(invalidStructure);

            assertThat(valid).isFalse();
        }
    }

    // =========================================================================
    // Token Validation - Expired Token Tests
    // =========================================================================

    @Nested
    @DisplayName("Token Validation - Expired Tokens")
    class ExpiredTokenTests {

        @Test
        @DisplayName("validates that expired token is rejected")
        void validateJwtToken_rejectsExpiredToken() {
            // Create a token that expired 1 hour ago
            String expiredToken = Jwts.builder()
                    .subject("expiredUser")
                    .id(UUID.randomUUID().toString())
                    .issuedAt(new Date(System.currentTimeMillis() - Duration.ofHours(2).toMillis()))
                    .expiration(new Date(System.currentTimeMillis() - Duration.ofHours(1).toMillis()))
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret)))
                    .compact();

            boolean valid = jwtUtils.validateJwtToken(expiredToken);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("validates that token expiring just now is rejected")
        void validateJwtToken_rejectsJustExpiredToken() {
            // Create a token that expired 1 second ago
            String expiredToken = Jwts.builder()
                    .subject("justExpiredUser")
                    .id(UUID.randomUUID().toString())
                    .issuedAt(new Date(System.currentTimeMillis() - Duration.ofMinutes(10).toMillis()))
                    .expiration(new Date(System.currentTimeMillis() - 1000))
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret)))
                    .compact();

            boolean valid = jwtUtils.validateJwtToken(expiredToken);

            assertThat(valid).isFalse();
        }
    }

    // =========================================================================
    // Token Validation - Tampered Token Tests
    // =========================================================================

    @Nested
    @DisplayName("Token Validation - Tampered Tokens")
    class TamperedTokenTests {

        @Test
        @DisplayName("validates that token signed with different key is rejected")
        void validateJwtToken_rejectsWrongSigningKey() {
            // Create a token with a different secret
            String differentSecret = Base64.getEncoder().encodeToString(
                    "this-is-a-completely-different-secret-key-for-signing".getBytes()
            );

            String tokenWithDifferentKey = Jwts.builder()
                    .subject("hackerUser")
                    .id(UUID.randomUUID().toString())
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + Duration.ofHours(1).toMillis()))
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(differentSecret)))
                    .compact();

            boolean valid = jwtUtils.validateJwtToken(tokenWithDifferentKey);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("validates that token with modified payload is rejected")
        void validateJwtToken_rejectsModifiedPayload() {
            // Generate a valid token
            String validToken = jwtUtils.generateTokenFromUserName("originalUser");

            // Split the token and modify the payload
            String[] parts = validToken.split("\\.");
            assertThat(parts).hasSize(3);

            // Decode, modify, and re-encode the payload (but keep original signature)
            String modifiedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    "{\"sub\":\"hackedUser\",\"iat\":1234567890,\"exp\":9999999999}".getBytes()
            );

            String tamperedToken = parts[0] + "." + modifiedPayload + "." + parts[2];

            boolean valid = jwtUtils.validateJwtToken(tamperedToken);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("validates that token with modified header is rejected")
        void validateJwtToken_rejectsModifiedHeader() {
            String validToken = jwtUtils.generateTokenFromUserName("user1");

            String[] parts = validToken.split("\\.");
            assertThat(parts).hasSize(3);

            // Modify the header
            String modifiedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes()
            );

            String tamperedToken = modifiedHeader + "." + parts[1] + "." + parts[2];

            boolean valid = jwtUtils.validateJwtToken(tamperedToken);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("validates that token with stripped signature is rejected")
        void validateJwtToken_rejectsStrippedSignature() {
            String validToken = jwtUtils.generateTokenFromUserName("user1");

            String[] parts = validToken.split("\\.");
            assertThat(parts).hasSize(3);

            // Remove signature (alg=none attack)
            String unsignedToken = parts[0] + "." + parts[1] + ".";

            boolean valid = jwtUtils.validateJwtToken(unsignedToken);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("validates that token with corrupted signature is rejected")
        void validateJwtToken_rejectsCorruptedSignature() {
            String validToken = jwtUtils.generateTokenFromUserName("user1");

            String[] parts = validToken.split("\\.");
            assertThat(parts).hasSize(3);

            // Corrupt the signature by changing some characters
            String corruptedSignature = parts[2].substring(0, parts[2].length() - 5) + "XXXXX";
            String tamperedToken = parts[0] + "." + parts[1] + "." + corruptedSignature;

            boolean valid = jwtUtils.validateJwtToken(tamperedToken);

            assertThat(valid).isFalse();
        }
    }

    // =========================================================================
    // Token Claims Extraction Tests
    // =========================================================================

    @Nested
    @DisplayName("Token Claims Extraction")
    class ClaimsExtractionTests {

        @Test
        @DisplayName("extracts username correctly from valid token")
        void getUserNameFromJWTToken_extractsUsername() {
            String token = jwtUtils.generateTokenFromUserName("testuser");

            String username = jwtUtils.getUserNameFromJWTToken(token);

            assertThat(username).isEqualTo("testuser");
        }

        @Test
        @DisplayName("extracts JTI correctly from valid token")
        void getJtiFromToken_extractsJti() {
            String token = jwtUtils.generateTokenFromUserName("testuser");

            String jti = jwtUtils.getJtiFromToken(token);

            assertThat(jti).isNotBlank();
            // JTI should be a valid UUID format
            assertThat(UUID.fromString(jti)).isNotNull();
        }

        @Test
        @DisplayName("extracts username with special characters")
        void getUserNameFromJWTToken_handlesSpecialCharacters() {
            String usernameWithSpecialChars = "user+test@example.com";
            String token = jwtUtils.generateTokenFromUserName(usernameWithSpecialChars);

            String extracted = jwtUtils.getUserNameFromJWTToken(token);

            assertThat(extracted).isEqualTo(usernameWithSpecialChars);
        }

        @Test
        @DisplayName("extracts username with unicode characters")
        void getUserNameFromJWTToken_handlesUnicodeCharacters() {
            String unicodeUsername = "用户名_тест_🔐";
            String token = jwtUtils.generateTokenFromUserName(unicodeUsername);

            String extracted = jwtUtils.getUserNameFromJWTToken(token);

            assertThat(extracted).isEqualTo(unicodeUsername);
        }
    }

    // =========================================================================
    // Edge Cases Tests
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("handles very long username")
        void handlesVeryLongUsername() {
            String longUsername = "a".repeat(1000);
            String token = jwtUtils.generateTokenFromUserName(longUsername);

            assertThat(jwtUtils.validateJwtToken(token)).isTrue();
            assertThat(jwtUtils.getUserNameFromJWTToken(token)).isEqualTo(longUsername);
        }

        @Test
        @DisplayName("handles empty string username - JWT library returns null for empty subject")
        void handlesEmptyUsername() {
            String token = jwtUtils.generateTokenFromUserName("");

            assertThat(jwtUtils.validateJwtToken(token)).isTrue();
            // JWT library treats empty string as null/absent subject
            assertThat(jwtUtils.getUserNameFromJWTToken(token)).isNull();
        }

        @Test
        @DisplayName("handles username with whitespace only - JWT library trims to null")
        void handlesWhitespaceUsername() {
            String whitespaceUsername = "   ";
            String token = jwtUtils.generateTokenFromUserName(whitespaceUsername);

            assertThat(jwtUtils.validateJwtToken(token)).isTrue();
            // JWT library treats whitespace-only string as null/absent subject
            assertThat(jwtUtils.getUserNameFromJWTToken(token)).isNull();
        }
    }
}
