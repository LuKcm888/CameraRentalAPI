package com.camerarental.backend.security.jwt;

import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthEntryPointJwtTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final AuthEntryPointJwt entryPoint = new AuthEntryPointJwt(jsonMapper);

    @Test
    @DisplayName("commence should return 401 with JSON body containing status, message, and path")
    void commence_setsUnauthorizedJsonResponse() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/projects");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationException ex = new BadCredentialsException("Bad credentials");

        // Act
        entryPoint.commence(request, response, ex);

        // Assert HTTP status + content type
        assertThat(response.getStatus())
                .isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentType())
                .isEqualTo("application/json");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = jsonMapper.readValue(
                response.getContentAsByteArray(), Map.class
        );

        assertThat(body.get("status")).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(body.get("message")).isEqualTo("Unauthorized");
        assertThat(body.get("path")).isEqualTo("/api/v1/projects");
    }
}
