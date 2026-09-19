package com.ecommerce.user_service.service;

import com.ecommerce.user_service.client.RoleServiceClient;
import com.ecommerce.user_service.dto.request.UserRegistrationRequestDTO;
import com.ecommerce.user_service.dto.request.UserUpdateRequestDTO;
import com.ecommerce.user_service.dto.response.UserAuthDTO;
import com.ecommerce.user_service.dto.response.UserResponseDTO;
import com.ecommerce.user_service.entity.User;
import com.ecommerce.user_service.exception.EmailAlreadyExistsException;
import com.ecommerce.user_service.exception.InvalidRoleException;
import com.ecommerce.user_service.exception.UserNotFoundException;
import com.ecommerce.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleServiceClient roleServiceClient;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO registerUser(UserRegistrationRequestDTO request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        try {
            roleServiceClient.getRoleById(request.getRoleId());
        } catch (Exception e) {
            // Assuming 404 from Role Service
            throw new InvalidRoleException(request.getRoleId());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .roleId(request.getRoleId())
                .build();

        User savedUser = userRepository.save(user);
        return mapToResponseDTO(savedUser);
    }

    public UserResponseDTO getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        return mapToResponseDTO(user);
    }

    @Transactional
    public UserResponseDTO updateProfile(UUID userId, UserUpdateRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        user.setFullName(request.getFullName());
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        User updatedUser = userRepository.save(user);
        return mapToResponseDTO(updatedUser);
    }

    public UserAuthDTO lookupUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return UserAuthDTO.builder()
                .id(user.getId())
                .passwordHash(user.getPasswordHash())
                .roleId(user.getRoleId())
                .build();
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleId(user.getRoleId())
                .build();
    }
}
