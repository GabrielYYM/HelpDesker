package com.corp.DeskHelper.service;

import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import com.corp.DeskHelper.dto.FirstAccessRequestDTO;
import com.corp.DeskHelper.dto.LoginRequestDTO;
import com.corp.DeskHelper.dto.UserResponseDTO;
import com.corp.DeskHelper.model.User;
import com.corp.DeskHelper.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO login(LoginRequestDTO request, HttpServletRequest httpRequest) {
        User user = userRepository.findByCode(request.code())
            .orElseThrow(() -> new BadCredentialsException("Código ou senha inválidos"));

        if (!user.isActive()) {
            throw new DisabledException("Usuário inativo no sistema");
        }

        if (user.isFirstAccess() || user.getPassword() == null) {
            throw new BadCredentialsException("Primeiro acesso pendente: defina sua senha inicial");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Código ou senha inválidos");
        }

        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(user.getCode(), null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute("USER_ID", user.getId());

        return UserResponseDTO.fromEntity(user);
    }

    public UserResponseDTO firstAccess(FirstAccessRequestDTO request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("As senhas não coincidem");
        }

        User user = userRepository.findByCode(request.code())
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!user.isFirstAccess()) {
            throw new IllegalStateException("O primeiro acesso para este usuário já foi realizado");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFirstAccess(false);
        userRepository.save(user);

        return UserResponseDTO.fromEntity(user);
    }

    public UserResponseDTO getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BadCredentialsException("Usuário não autenticado");
        }

        String code = auth.getName();
        User user = userRepository.findByCode(code)
            .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));

        return UserResponseDTO.fromEntity(user);
    }
}
