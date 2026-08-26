package com.ecommerce.role_service.repository;

import com.ecommerce.role_service.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldSaveAndFindRoleById() {
        Role role = Role.builder()
                .name("ROLE_USER")
                .description("Standard user role")
                .build();

        Role savedRole = roleRepository.save(role);

        Optional<Role> foundRole = roleRepository.findById(savedRole.getId());
        assertTrue(foundRole.isPresent());
        assertEquals("ROLE_USER", foundRole.get().getName());
    }

    @Test
    void shouldFindRoleByName() {
        Role role = Role.builder()
                .name("ROLE_ADMIN")
                .description("Administrator role")
                .build();

        roleRepository.save(role);

        Optional<Role> foundRole = roleRepository.findByName("ROLE_ADMIN");
        assertTrue(foundRole.isPresent());
        assertEquals("ROLE_ADMIN", foundRole.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenRoleByNameNotFound() {
        Optional<Role> foundRole = roleRepository.findByName("NON_EXISTENT");
        assertFalse(foundRole.isPresent());
    }
}
