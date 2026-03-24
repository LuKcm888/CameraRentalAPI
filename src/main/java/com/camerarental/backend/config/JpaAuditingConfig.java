package com.camerarental.backend.config;

import com.camerarental.backend.security.services.UserDetailsImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Enables Spring Data JPA Auditing so that entities extending
 * {@link com.camerarental.backend.model.base.AuditableEntity} have their
 * {@code @CreatedDate}, {@code @LastModifiedDate}, and {@code @CreatedBy}
 * fields populated automatically on persist and update.
 *
 * <p>Without this configuration the {@code AuditingEntityListener} registered
 * on {@code AuditableEntity} has no {@link AuditorAware} to consult, which
 * leaves {@code createdBy} null and causes a NOT-NULL constraint violation
 * in PostgreSQL.</p>
 *
 * <p>The {@link #auditorProvider()} bean resolves the current user's UUID
 * from the Spring Security context, tying every audited row back to the
 * authenticated user who created it.</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Supplies the current authenticated user's UUID to Spring Data's
     * {@code @CreatedBy} annotation at persist time.
     *
     * <p>Returns {@link Optional#empty()} for unauthenticated or anonymous
     * requests (e.g. the data seeder's {@code CommandLineRunner}), in which
     * case {@code createdBy} must be set manually.</p>
     */
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.empty();
            }
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            return Optional.ofNullable(userDetails.getId());
        };
    }
}
