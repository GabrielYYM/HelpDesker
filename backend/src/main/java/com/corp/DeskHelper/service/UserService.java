package com.corp.DeskHelper.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.corp.DeskHelper.dto.UserCredentialDTO;
import com.corp.DeskHelper.dto.UserCreateRequestDTO;
import com.corp.DeskHelper.dto.UserResponseDTO;
import com.corp.DeskHelper.model.User;
import com.corp.DeskHelper.model.enums.UserRole;
import com.corp.DeskHelper.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String USER_CODE_SEQUENCE = "user-code";
    private static final String COUNTERS_COLLECTION = "counters";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoOperations mongoOperations;

    public String generateNextUserCode() {
        Document counter = mongoOperations.findAndModify(
            Query.query(Criteria.where("_id").is(USER_CODE_SEQUENCE)),
            new Update().inc("value", 1),
            FindAndModifyOptions.options().upsert(true).returnNew(true),
            Document.class,
            COUNTERS_COLLECTION
        );
        Object value = counter == null ? null : counter.get("value");
        if (!(value instanceof Number number) || number.longValue() < 1) {
            throw new IllegalStateException("Não foi possível gerar o próximo código de usuário.");
        }
        return String.format("%04d", number.longValue());
    }

    public UserCredentialDTO createUser(UserCreateRequestDTO request) {
        String temporaryPassword = generateTemporaryPassword();
        User user = User.builder()
            .code(generateNextUserCode())
            .name(request.name().trim())
            .password(passwordEncoder.encode(temporaryPassword))
            .role(request.role() != null ? request.role() : UserRole.USER)
            .firstAccess(true)
            .active(true)
            .build();

        return new UserCredentialDTO(UserResponseDTO.fromEntity(userRepository.save(user)), temporaryPassword);
    }

    public UserCredentialDTO reissueTemporaryPassword(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!user.isActive() || !user.isFirstAccess()) {
            throw new IllegalStateException("Só é possível gerar uma senha temporária para uma conta ativa com primeiro acesso pendente.");
        }
        String temporaryPassword = generateTemporaryPassword();
        if (userRepository.replaceTemporaryPassword(id, user.getPassword(), passwordEncoder.encode(temporaryPassword)) != 1) {
            throw new IllegalStateException("A conta foi atualizada. Recarregue e tente novamente.");
        }
        return new UserCredentialDTO(UserResponseDTO.fromEntity(user), temporaryPassword);
    }

    private String generateTemporaryPassword() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
            .map(UserResponseDTO::fromEntity)
            .toList();
    }
}
