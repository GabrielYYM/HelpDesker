package com.corp.DeskHelper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FirstAccessRequestDTO(
    @NotBlank(message = "A nova senha é obrigatória")
    @Size(min = 6, max = 128, message = "A senha deve ter entre 6 e 128 caracteres")
    String newPassword,

    @NotBlank(message = "A confirmação da senha é obrigatória")
    String confirmPassword
) {}
