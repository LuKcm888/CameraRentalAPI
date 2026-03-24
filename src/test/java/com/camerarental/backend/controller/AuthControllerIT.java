package com.camerarental.backend.controller;


import com.camerarental.backend.config.AbstractPostgresIT;
import com.camerarental.backend.config.ApiPaths;
import com.camerarental.backend.config.PostgresIntegrationTest;
import com.camerarental.backend.model.entity.User;
import com.camerarental.backend.model.entity.enums.AppRole;
import com.camerarental.backend.security.request.LoginRequest;
import com.camerarental.backend.security.request.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PostgresIntegrationTest
class AuthControllerIT extends AbstractPostgresIT {

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // /signup
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST " + ApiPaths.AUTH + "/signup - registerUser")
    class SignupTests {

        @Test
        @DisplayName("should return 201 Created and create user with ROLE_CUSTOMER")
        void signup_success() throws Exception {
            ensureCustomerRole();

            SignupRequest request = new SignupRequest();
            request.setUsername("user1");
            request.setEmail("user1@example.com");
            request.setPassword("Password123!");

            mockMvc.perform(post(ApiPaths.AUTH + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("User registered successfully!"));

            inTransaction(() -> {
                var userOpt = userRepository.findByUserName("user1");
                assertThat(userOpt).isPresent();

                User user = userOpt.get();
                assertThat(user.getEmail()).isEqualTo("user1@example.com");
                assertThat(passwordEncoder.matches("Password123!", user.getPassword())).isTrue();
                assertThat(user.getRoles())
                        .extracting(r -> r.getRoleName())
                        .contains(AppRole.ROLE_CUSTOMER);
            });
        }

        @Test
        @DisplayName("should return 400 when username already exists")
        void signup_usernameTaken() throws Exception {
            ensureCustomerRole();
            createUserWithRole("user1", "Password123!", "user1@example.com", AppRole.ROLE_CUSTOMER);

            SignupRequest request = new SignupRequest();
            request.setUsername("user1");
            request.setEmail("another@example.com");
            request.setPassword("Password123!");

            mockMvc.perform(post(ApiPaths.AUTH + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Error: Username is already taken!"));
        }

        @Test
        @DisplayName("should return 400 when email already exists")
        void signup_emailTaken() throws Exception {
            ensureCustomerRole();
            createUserWithRole("user1", "Password123!", "user1@example.com", AppRole.ROLE_CUSTOMER);

            SignupRequest request = new SignupRequest();
            request.setUsername("otherUser");
            request.setEmail("user1@example.com");
            request.setPassword("Password123!");

            mockMvc.perform(post(ApiPaths.AUTH + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Error: Email is already taken!"));
        }
    }

    // -------------------------------------------------------------------------
    // /signin
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST " + ApiPaths.AUTH + "/signin - authenticateUser")
    class SigninTests {

        @Test
        @DisplayName("should return 200 OK, Bearer token, and user info on valid credentials")
        void signin_success() throws Exception {
            ensureCustomerRole();
            createUserWithRole("user1", "Password123!", "user1@example.com", AppRole.ROLE_CUSTOMER);

            LoginRequest request = new LoginRequest();
            request.setUsername("user1");
            request.setPassword("Password123!");

            mockMvc.perform(post(ApiPaths.AUTH + "/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.user.id").isString())
                    .andExpect(jsonPath("$.user.username").value("user1"))
                    .andExpect(jsonPath("$.user.roles[0]").value("ROLE_CUSTOMER"));
        }

        @Test
        @DisplayName("should return 401 on bad credentials")
        void signin_badCredentials_returns401() throws Exception {
            ensureCustomerRole();
            createUserWithRole("user1", "Password123!", "user1@example.com", AppRole.ROLE_CUSTOMER);

            LoginRequest request = new LoginRequest();
            request.setUsername("user1");
            request.setPassword("wrong-pass");

            mockMvc.perform(post(ApiPaths.AUTH + "/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Bad credentials"))
                    .andExpect(jsonPath("$.status").value(false));
        }
    }

    // -------------------------------------------------------------------------
    // /signout
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST " + ApiPaths.AUTH + "/signout - blacklist token")
    class SignoutTests {

        @Test
        @DisplayName("should return 200 even without Authorization header")
        void signout_withoutHeader_returnsOk() throws Exception {
            mockMvc.perform(post(ApiPaths.AUTH + "/signout")
                            .with(user("tester")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }
    }
}
