package com.corp.DeskHelper.controller;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.corp.DeskHelper.dto.UserCreateRequestDTO;
import com.corp.DeskHelper.dto.UserCredentialDTO;
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
    @PreAuthorize("hasRole('ADM') or (hasRole('IT') and #p0.role() != T(com.corp.DeskHelper.model.enums.UserRole).ADM)")
    public ResponseEntity<UserCredentialDTO> createUser(@Valid @RequestBody UserCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(userService.createUser(request));
    }

    @PostMapping("/{id}/temporary-password")
    @PreAuthorize("hasRole('ADM')")
    public ResponseEntity<UserCredentialDTO> reissueTemporaryPassword(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(userService.reissueTemporaryPassword(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('IT', 'ADM')")
    public ResponseEntity<List<UserResponseDTO>> listUsers() {
        return ResponseEntity.ok(userService.findAll());
    }
}
