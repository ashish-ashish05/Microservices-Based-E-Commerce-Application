package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.dto.request.UserRegistrationRequestDTO;
import com.ecommerce.user_service.dto.request.UserUpdateRequestDTO;
import com.ecommerce.user_service.dto.response.UserAuthDTO;
import com.ecommerce.user_service.dto.response.UserResponseDTO;
import com.ecommerce.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegistrationRequestDTO request) {
        return new ResponseEntity<>(userService.registerUser(request), HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequestDTO request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @GetMapping("/lookup")
    public ResponseEntity<UserAuthDTO> lookup(@RequestParam String email) {
        return ResponseEntity.ok(userService.lookupUser(email));
    }
}
