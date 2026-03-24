package com.camerarental.backend.repository;



import com.camerarental.backend.model.entity.Role;
import com.camerarental.backend.model.entity.enums.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data-access layer for {@link Role} entities.
 *
 * <p>Used during user registration and by the data seeder to look up
 * roles by their {@link AppRole} enum value so they can be assigned to
 * new users.</p>
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(AppRole appRole);
}
