package com.camerarental.backend.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Per-request filter that extracts and validates a JWT from the
 * {@code Authorization: Bearer <token>} header.
 *
 * <p>If the token is present, valid, and not blacklisted, the filter
 * loads the corresponding {@link UserDetails} via
 * {@link UserDetailsService} and places a fully authenticated
 * {@link UsernamePasswordAuthenticationToken} into the
 * {@link SecurityContextHolder}. Downstream components
 * ({@code @PreAuthorize}, {@code @AuthenticationPrincipal},
 * {@code AuditorAware}) can then access the current user.</p>
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee it runs exactly
 * once per request, even through internal forwards or error
 * dispatches.</p>
 */
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final JwtBlacklistService jwtBlacklistService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        logger.debug("AuthTokenFilter called for URI {}", request.getRequestURI());

        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String jti = jwtUtils.getJtiFromToken(jwt);

                if (jwtBlacklistService.isBlacklisted(jti)) {
                    logger.warn("Blocked blacklisted JWT for jti={}", jti);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token is invalidated. Please sign in again.");
                    return;
                }

                String username = jwtUtils.getUserNameFromJWTToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Roles from JWT: {}", userDetails.getAuthorities());
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication", e);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Read token from Authorization: Bearer <token> header.
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            String token = headerAuth.substring(7);
            logger.debug("AuthTokenFilter.java: tokenPresent={}", true);
            return token;
        }
        logger.debug("AuthTokenFilter.java: tokenPresent={}", false);
        return null;
    }
}
