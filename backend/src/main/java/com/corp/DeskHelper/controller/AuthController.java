package com.corp.DeskHelper.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.corp.DeskHelper.dto.FirstAccessRequestDTO;
import com.corp.DeskHelper.dto.LoginRequestDTO;
import com.corp.DeskHelper.dto.UserResponseDTO;
import com.corp.DeskHelper.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public ResponseEntity<UserResponseDTO> login(@Valid @RequestBody LoginRequestDTO request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/auth/first-access")
    public ResponseEntity<UserResponseDTO> firstAccess(@Valid @RequestBody FirstAccessRequestDTO request) {
        return ResponseEntity.ok(authService.firstAccess(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
