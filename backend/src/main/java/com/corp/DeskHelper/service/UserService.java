package com.corp.DeskHelper.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.corp.DeskHelper.dto.UserCreateRequestDTO;
import com.corp.DeskHelper.dto.UserResponseDTO;
import com.corp.DeskHelper.model.User;
import com.corp.DeskHelper.model.enums.UserRole;
import com.corp.DeskHelper.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public synchronized String generateNextUserCode() {
        return userRepository.findFirstByOrderByCodeDesc()
            .map(user -> {
                try {
                    long nextNumber = Long.parseLong(user.getCode()) + 1;
                    return String.format("%04d", nextNumber);
                } catch (NumberFormatException e) {
                    return "0001";
                }
            })
            .orElse("0001");
    }

    public UserResponseDTO createUser(UserCreateRequestDTO request) {
        User user = User.builder()
            .code(generateNextUserCode())
            .name(request.name().trim())
            .role(request.role() != null ? request.role() : UserRole.USER)
            .firstAccess(true)
            .active(true)
            .build();

        return UserResponseDTO.fromEntity(userRepository.save(user));
    }

    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
            .map(UserResponseDTO::fromEntity)
            .toList();
    }
}
