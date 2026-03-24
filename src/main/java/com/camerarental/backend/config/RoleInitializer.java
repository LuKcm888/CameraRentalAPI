package com.camerarental.backend.config;

import com.camerarental.backend.model.entity.Role;
import com.camerarental.backend.model.entity.enums.AppRole;
import com.camerarental.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures all {@link AppRole} values exist in the {@code roles} table on
 * every application startup, regardless of the active Spring profile.
 *
 * <p>Roles are reference data required for signup and authorization to
 * function.  Unlike test seed data (users, cameras, etc.) which is
 * profile-specific, roles must always be present.</p>
 *
 * <p>Runs with {@code @Order(1)} so that profile-specific seeders
 * (e.g. {@link LocalPgDataSeeder}) can depend on roles already existing.</p>
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RoleInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        for (AppRole appRole : AppRole.values()) {
            if (roleRepository.findByRoleName(appRole).isEmpty()) {
                roleRepository.save(new Role(appRole));
                created++;
            }
        }

        if (created > 0) {
            log.info("Initialized {} missing role(s)", created);
        } else {
            log.debug("All roles already present");
        }
    }
}
