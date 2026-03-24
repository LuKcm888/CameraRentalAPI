package com.camerarental.backend.security.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SignupRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private SignupRequest validRequest() {
        SignupRequest req = new SignupRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("Password1!");
        return req;
    }

    @Test
    @DisplayName("Valid signup request has no violations")
    void validRequest_hasNoViolations() {
        SignupRequest req = validRequest();
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Password Pattern Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @DisplayName("Valid passwords pass pattern validation")
    @ValueSource(strings = {
        "Password1!",
        "MyP@ssw0rd",
        "Test123$",
        "Abcdefg1&"
    })
    void validPasswords_passValidation(String password) {
        SignupRequest req = validRequest();
        req.setPassword(password);
        
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(req);
        
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Password without uppercase fails")
    void passwordNoUppercase_hasViolation() {
        SignupRequest req = validRequest();
        req.setPassword("password1!");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Password without lowercase fails")
    void passwordNoLowercase_hasViolation() {
        SignupRequest req = validRequest();
        req.setPassword("PASSWORD1!");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Password without digit fails")
    void passwordNoDigit_hasViolation() {
        SignupRequest req = validRequest();
        req.setPassword("Password!");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Password without special character fails")
    void passwordNoSpecialChar_hasViolation() {
        SignupRequest req = validRequest();
        req.setPassword("Password1");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Password too short fails")
    void passwordTooShort_hasViolation() {
        SignupRequest req = validRequest();
        req.setPassword("Pass1!");  // only 6 chars

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Invalid email fails validation")
    void invalidEmail_hasViolation() {
        SignupRequest req = validRequest();
        req.setEmail("not-an-email");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Blank username fails validation")
    void blankUsername_hasViolation() {
        SignupRequest req = validRequest();
        req.setUsername("   ");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("username"));
    }
}