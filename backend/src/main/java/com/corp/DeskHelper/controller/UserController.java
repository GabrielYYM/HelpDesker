package com.corp.DeskHelper.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.corp.DeskHelper.dto.UserCreateRequestDTO;
import com.corp.DeskHelper.dto.UserResponseDTO;
import com.corp.DeskHelper.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRADOR')")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRADOR')")
    public ResponseEntity<List<UserResponseDTO>> listUsers() {
        return ResponseEntity.ok(userService.findAll());
    }
}
