package com.corp.DeskHelper.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank(message = "O código é obrigatório")
    String code,

    @NotBlank(message = "A senha é obrigatória")
    String password
) {}
