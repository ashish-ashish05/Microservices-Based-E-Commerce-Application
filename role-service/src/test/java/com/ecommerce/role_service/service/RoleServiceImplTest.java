package com.ecommerce.role_service.service;

import com.ecommerce.role_service.dto.RoleRequest;
import com.ecommerce.role_service.dto.RoleResponse;
import com.ecommerce.role_service.entity.Role;
import com.ecommerce.role_service.exception.RoleAlreadyExistsException;
import com.ecommerce.role_service.exception.RoleNotFoundException;
import com.ecommerce.role_service.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void createRole_ShouldSaveRole_WhenNameIsUnique() {
        RoleRequest request = RoleRequest.builder()
                .name("ROLE_USER")
                .description("User role")
                .build();

        Role role = Role.builder()
                .id(UUID.randomUUID())
                .name("ROLE_USER")
                .description("User role")
                .build();

        when(roleRepository.findByName(request.getName())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        RoleResponse response = roleService.createRole(request);

        assertNotNull(response);
        assertEquals("ROLE_USER", response.getName());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void createRole_ShouldThrowException_WhenNameExists() {
        RoleRequest request = RoleRequest.builder()
                .name("ROLE_ADMIN")
                .build();

        Role existingRole = Role.builder()
                .id(UUID.randomUUID())
                .name("ROLE_ADMIN")
                .build();

        when(roleRepository.findByName(request.getName())).thenReturn(Optional.of(existingRole));

        assertThrows(RoleAlreadyExistsException.class, () -> roleService.createRole(request));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void getRole_ShouldReturnRole_WhenIdExists() {
        UUID id = UUID.randomUUID();
        Role role = Role.builder()
                .id(id)
                .name("ROLE_USER")
                .build();

        when(roleRepository.findById(id)).thenReturn(Optional.of(role));

        RoleResponse response = roleService.getRole(id);

        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("ROLE_USER", response.getName());
    }

    @Test
    void getRole_ShouldThrowException_WhenIdNotFound() {
        UUID id = UUID.randomUUID();
        when(roleRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class, () -> roleService.getRole(id));
    }

    @Test
    void getAllRoles_ShouldReturnList() {
        List<Role> roles = List.of(
                Role.builder().id(UUID.randomUUID()).name("ROLE_USER").build(),
                Role.builder().id(UUID.randomUUID()).name("ROLE_ADMIN").build()
        );

        when(roleRepository.findAll()).thenReturn(roles);

        List<RoleResponse> responses = roleService.getAllRoles();

        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(r -> r.getName().equals("ROLE_USER")));
        assertTrue(responses.stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")));
    }

    @Test
    void updateRole_ShouldUpdateRole_WhenIdExistsAndNameIsUnique() {
        UUID id = UUID.randomUUID();
        Role existingRole = Role.builder()
                .id(id)
                .name("ROLE_OLD")
                .build();
        RoleRequest request = RoleRequest.builder()
                .name("ROLE_NEW")
                .build();

        Role updatedRole = Role.builder()
                .id(id)
                .name("ROLE_NEW")
                .build();

        when(roleRepository.findById(id)).thenReturn(Optional.of(existingRole));
        when(roleRepository.findByName("ROLE_NEW")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(updatedRole);

        RoleResponse response = roleService.updateRole(id, request);

        assertEquals("ROLE_NEW", response.getName());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void updateRole_ShouldThrowException_WhenNewNameExists() {
        UUID id = UUID.randomUUID();
        Role existingRole = Role.builder()
                .id(id)
                .name("ROLE_OLD")
                .build();
        RoleRequest request = RoleRequest.builder()
                .name("ROLE_CONFLICT")
                .build();

        Role conflictRole = Role.builder()
                .id(UUID.randomUUID())
                .name("ROLE_CONFLICT")
                .build();

        when(roleRepository.findById(id)).thenReturn(Optional.of(existingRole));
        when(roleRepository.findByName("ROLE_CONFLICT")).thenReturn(Optional.of(conflictRole));

        assertThrows(RoleAlreadyExistsException.class, () -> roleService.updateRole(id, request));
    }

    @Test
    void deleteRole_ShouldCallDelete_WhenIdExists() {
        UUID id = UUID.randomUUID();
        when(roleRepository.existsById(id)).thenReturn(true);

        roleService.deleteRole(id);

        verify(roleRepository).deleteById(id);
    }

    @Test
    void deleteRole_ShouldThrowException_WhenIdNotFound() {
        UUID id = UUID.randomUUID();
        when(roleRepository.existsById(id)).thenReturn(false);

        assertThrows(RoleNotFoundException.class, () -> roleService.deleteRole(id));
    }
}
