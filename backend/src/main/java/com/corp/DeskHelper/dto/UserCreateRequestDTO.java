package com.corp.DeskHelper.dto;

import com.corp.DeskHelper.model.enums.UserRole;

import jakarta.validation.constraints.NotBlank;

public record UserCreateRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    String name,

    UserRole role
) {}
