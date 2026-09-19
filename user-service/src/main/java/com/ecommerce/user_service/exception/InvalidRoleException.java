package com.ecommerce.user_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@Getter
public class InvalidRoleException extends RuntimeException {
    private final UUID roleId;
    private final HttpStatus status = HttpStatus.NOT_FOUND;

    public InvalidRoleException(UUID roleId) {
        super("Invalid role assigned");
        this.roleId = roleId;
    }
}
