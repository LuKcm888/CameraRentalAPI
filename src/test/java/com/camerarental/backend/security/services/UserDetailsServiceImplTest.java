package com.camerarental.backend.security.services;


import com.camerarental.backend.model.entity.User;
import com.camerarental.backend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl service;

    @Test
    @DisplayName("loadUserByUsername - returns UserDetails when user exists")
    void loadUserByUsername_success() {
        User user = new User("user1", "user1@example.com", "encoded");
        UUID userId = UUID.randomUUID();
        user.setUserId(userId);

        given(userRepository.findByUserName("user1")).willReturn(Optional.of(user));

        var result = service.loadUserByUsername("user1");

        assertThat(result).isInstanceOf(UserDetailsImpl.class);
        UserDetailsImpl details = (UserDetailsImpl) result;
        assertThat(details.getId()).isEqualTo(userId);
        assertThat(details.getUsername()).isEqualTo("user1");
        assertThat(details.getEmail()).isEqualTo("user1@example.com");
    }

    @Test
    @DisplayName("loadUserByUsername - throws when user not found")
    void loadUserByUsername_notFound() {
        given(userRepository.findByUserName("missing")).willReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing"));
    }
}
