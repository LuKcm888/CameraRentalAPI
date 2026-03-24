package com.camerarental.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class OriginCheckFilter extends OncePerRequestFilter {

    private final List<String> allowedOrigins;
    private final List<String> protectedPaths;

    public OriginCheckFilter(List<String> allowedOrigins, List<String> protectedPaths) {
        this.allowedOrigins = allowedOrigins;
        this.protectedPaths = protectedPaths;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // We only care about POSTs to the auth endpoints
        if (HttpMethod.POST.matches(method) && isProtectedPath(path)) {

            // Browsers usually send Origin; if missing, fall back to Referer
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");

            // If no Origin/Referer (Postman, curl, backend services), we let it pass.
            if (origin != null) {
                if (!isAllowedOrigin(origin)) {
                    log.warn("blocked_auth_request origin={} path={}", origin, path);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            } else if (referer != null) {
                // Optional: perform a very simple check on Referer host
                if (!allowedOrigins.stream().anyMatch(referer::startsWith)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }
        }

        // If everything is fine (or not a protected path), continue the chain
        filterChain.doFilter(request, response);
    }

    private boolean isProtectedPath(String path) {
        // e.g. "/api/v1/auth/signin"
        return protectedPaths.stream().anyMatch(path::startsWith);
    }

    private boolean isAllowedOrigin(String origin) {
        // Exact match or prefix match depending on your needs
        return allowedOrigins.contains(origin);
    }
}
