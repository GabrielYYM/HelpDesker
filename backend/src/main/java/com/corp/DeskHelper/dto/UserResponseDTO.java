package com.corp.DeskHelper.dto;

import com.corp.DeskHelper.model.User;
import com.corp.DeskHelper.model.enums.UserRole;

public record UserResponseDTO(
    String id,
    String code,
    String name,
    UserRole role,
    boolean firstAccess,
    boolean active
) {
    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
            user.getId(),
            user.getCode(),
            user.getName(),
            user.getRole(),
            user.isFirstAccess(),
            user.isActive()
        );
    }
}
