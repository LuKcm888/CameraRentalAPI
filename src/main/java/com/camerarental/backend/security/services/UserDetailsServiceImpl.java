package com.camerarental.backend.security.services;



import com.camerarental.backend.model.entity.User;
import com.camerarental.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges the JPA {@link User} entity with Spring Security's authentication
 * mechanism by implementing {@link UserDetailsService}.
 *
 * <p>Spring Security calls {@link #loadUserByUsername(String)} during
 * authentication (login) and JWT token validation to resolve a username
 * into a fully populated {@link UserDetailsImpl} that carries the user's
 * UUID, hashed password, and granted authorities.</p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with UserName " + username));


        return UserDetailsImpl.build(user);
    }
}
