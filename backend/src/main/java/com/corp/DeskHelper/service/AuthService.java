package com.corp.DeskHelper.service;

import java.util.List;
import java.util.Objects;

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

    private static final String TEMPORARY_HASH = "FIRST_ACCESS_HASH";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO login(LoginRequestDTO request, HttpServletRequest httpRequest) {
        User user = userRepository.findByCode(request.code())
            .orElseThrow(() -> new BadCredentialsException("Código ou senha inválidos"));

        if (!user.isActive()) {
            throw new DisabledException("Usuário inativo no sistema");
        }

        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Código ou senha inválidos");
        }

        startSession(user, httpRequest);
        return UserResponseDTO.fromEntity(user);
    }

    private void startSession(User user, HttpServletRequest httpRequest) {
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(user.isFirstAccess() ? "ROLE_FIRST_ACCESS" : "ROLE_" + user.getRole().name())
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(user.getCode(), null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute("USER_ID", user.getId());
        session.setAttribute(TEMPORARY_HASH, user.isFirstAccess() ? user.getPassword() : null);
    }

    public UserResponseDTO firstAccess(FirstAccessRequestDTO request, HttpServletRequest httpRequest) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("As senhas não coincidem");
        }

        HttpSession session = httpRequest.getSession(false);
        String temporaryHash = session == null ? null : (String) session.getAttribute(TEMPORARY_HASH);
        User user = currentUser();
        if (!user.isActive() || !user.isFirstAccess() || temporaryHash == null
                || !Objects.equals(temporaryHash, user.getPassword())) {
            throw new IllegalStateException("A senha temporária mudou ou deixou de ser válida. Entre novamente.");
        }
        if (passwordEncoder.matches(request.newPassword(), temporaryHash)) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha temporária.");
        }
        if (userRepository.completeFirstAccess(user.getId(), temporaryHash, passwordEncoder.encode(request.newPassword())) != 1) {
            throw new IllegalStateException("A senha temporária mudou ou deixou de ser válida. Entre novamente.");
        }
        session.invalidate();
        user.setFirstAccess(false);
        startSession(user, httpRequest);
        return UserResponseDTO.fromEntity(user);
    }

    public UserResponseDTO getCurrentUser() {
        User user = currentUser();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!user.isFirstAccess() && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_FIRST_ACCESS"))) {
            throw new BadCredentialsException("Primeiro acesso concluído. Entre novamente.");
        }
        return UserResponseDTO.fromEntity(user);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BadCredentialsException("Usuário não autenticado");
        }

        String code = auth.getName();
        return userRepository.findByCode(code)
            .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
    }
}
