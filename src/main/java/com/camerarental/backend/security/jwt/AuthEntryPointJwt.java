package com.camerarental.backend.security.jwt;

import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom {@link AuthenticationEntryPoint} that is invoked whenever an
 * unauthenticated request reaches a protected endpoint.
 *
 * <p>Instead of redirecting to a login page (the default for form-based
 * auth), this returns a {@code 401 Unauthorized} JSON response so that
 * API clients receive a machine-readable error they can act on.</p>
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);
    private final JsonMapper jsonMapper;

    public AuthEntryPointJwt(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("message", "Unauthorized");
        body.put("path", request.getServletPath());

        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
