package com.camerarental.backend.security.services;


import com.camerarental.backend.model.entity.Role;
import com.camerarental.backend.model.entity.User;
import com.camerarental.backend.model.entity.enums.AppRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailsImplTest {

    @Test
    @DisplayName("build(User) should map fields and roles correctly")
    void build_shouldMapUserToUserDetailsImpl() {
        // given
        User user = new User("user1", "user1@example.com", "encoded-pass");
        UUID userId = UUID.randomUUID();
        user.setUserId(userId);

        Role roleUser = new Role(AppRole.ROLE_CUSTOMER);
        Role roleAdmin = new Role(AppRole.ROLE_ADMIN);
        user.setRoles(Set.of(roleUser, roleAdmin));

        // when
        UserDetailsImpl details = UserDetailsImpl.build(user);

        // then
        assertThat(details.getId()).isEqualTo(userId);
        assertThat(details.getUsername()).isEqualTo("user1");
        assertThat(details.getEmail()).isEqualTo("user1@example.com");
        assertThat(details.getPassword()).isEqualTo("encoded-pass");

        List<String> authorityNames = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorityNames)
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("equals should compare by id only")
    void equals_shouldBeBasedOnId() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        UserDetailsImpl u1 = new UserDetailsImpl(
                id1, "user1", "user1@example.com", "pass", List.of()
        );
        UserDetailsImpl u2 = new UserDetailsImpl(
                id1, "anotherName", "another@example.com", "otherPass", List.of()
        );
        UserDetailsImpl u3 = new UserDetailsImpl(
                id2, "user2", "user2@example.com", "pass2", List.of()
        );

        assertThat(u1).isEqualTo(u2);          // same id
        assertThat(u1).isNotEqualTo(u3);       // different id
        assertThat(u1).isNotEqualTo(null);     // null-safe
        assertThat(u1).isNotEqualTo("string"); // different type
    }

    @Test
    @DisplayName("account flags should all return true by default")
    void accountFlags_shouldAllBeTrueByDefault() {
        UserDetailsImpl details = new UserDetailsImpl(
                UUID.randomUUID(), "user1", "user1@example.com", "pass", List.of()
        );

        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }
}
