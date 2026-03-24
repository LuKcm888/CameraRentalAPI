package com.camerarental.backend.repository;



import com.camerarental.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data-access layer for {@link User} entities.
 *
 * <p>Provides lookup by username (used during authentication) and
 * existence checks by username and email (used during registration
 * to enforce uniqueness).</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUserName(String username);

    boolean existsByUserName(String username);

    boolean existsByEmail(String email);
}
