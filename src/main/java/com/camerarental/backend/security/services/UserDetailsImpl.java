package com.camerarental.backend.security.services;


import com.camerarental.backend.model.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application-specific implementation of Spring Security's {@link UserDetails}.
 *
 * <p>Wraps the JPA {@link User} entity into a security-friendly object that
 * the framework uses throughout the authentication and authorization lifecycle
 * (JWT validation, {@code @PreAuthorize} checks, {@code @AuthenticationPrincipal}
 * injection, and the {@code AuditorAware} bean in
 * {@link com.camerarental.backend.config.JpaAuditingConfig}).</p>
 *
 * <p>Use the static {@link #build(User)} factory to convert a {@code User}
 * entity, which maps each assigned {@code Role} to a
 * {@link SimpleGrantedAuthority}.</p>
 */
@NoArgsConstructor
@Data
public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String username;
    private String email;

    @JsonIgnore
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(UUID id, String username, String email, String password,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName().name()))
                .collect(Collectors.toList());

        return new UserDetailsImpl(
          user.getUserId(),
          user.getUserName(),
          user.getEmail(),
          user.getPassword(),
          authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }


}
