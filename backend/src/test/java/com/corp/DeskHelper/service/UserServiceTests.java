package com.corp.DeskHelper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import com.corp.DeskHelper.dto.UserCreateRequestDTO;
import com.corp.DeskHelper.model.User;
import com.corp.DeskHelper.model.enums.UserRole;
import com.corp.DeskHelper.repository.UserRepository;

class UserServiceTests {

    private final UserRepository repository = mock(UserRepository.class);
    private final Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private final MongoOperations mongoOperations = mock(MongoOperations.class);
    private final UserService service = new UserService(repository, encoder, mongoOperations);

    @Test
    void firstUserStartsAtCode0001WithDefaultRoleAndPendingFirstAccess() {
        nextCodes(1L);
        var stored = ArgumentCaptor.forClass(User.class);
        when(repository.save(stored.capture())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertThat(user.getPassword()).startsWith("$argon2id$");
            return user;
        });

        var created = service.createUser(new UserCreateRequestDTO("  Pessoa de teste  ", null));

        assertThat(created.user().code()).isEqualTo("0001");
        assertThat(created.user().name()).isEqualTo("Pessoa de teste");
        assertThat(created.user().role()).isEqualTo(UserRole.USER);
        assertThat(created.user().active()).isTrue();
        assertThat(created.user().firstAccess()).isTrue();
        assertThat(encoder.matches(created.temporaryPassword(), stored.getValue().getPassword())).isTrue();
    }

    @Test
    void createUsesAtomicCounterAndKeepsRequestedRole() {
        nextCodes(43L);
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.createUser(new UserCreateRequestDTO("Equipe", UserRole.IT));

        assertThat(created.user().code()).isEqualTo("0043");
        assertThat(created.user().role()).isEqualTo(UserRole.IT);
    }

    @Test
    void shortLegacyCodesDoNotInfluenceTheNextCode() {
        nextCodes(2L, 3L);
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var first = service.createUser(new UserCreateRequestDTO("Primeira pessoa", UserRole.USER));
        var second = service.createUser(new UserCreateRequestDTO("Segunda pessoa", UserRole.USER));

        assertThat(first.user().code()).isEqualTo("0002");
        assertThat(second.user().code()).isEqualTo("0003");
    }

    @Test
    void administratorCanIssueTemporaryPasswordForLegacyPendingUser() {
        User legacy = User.builder().id("legacy").code("0042").name("Pessoa de teste").build();
        when(repository.findById("legacy")).thenReturn(Optional.of(legacy));
        var hash = ArgumentCaptor.forClass(String.class);
        when(repository.replaceTemporaryPassword(eq("legacy"), isNull(), hash.capture())).thenReturn(1L);

        var credentials = service.reissueTemporaryPassword("legacy");

        assertThat(credentials.user().firstAccess()).isTrue();
        assertThat(encoder.matches(credentials.temporaryPassword(), hash.getValue())).isTrue();
    }

    private void nextCodes(Long... values) {
        Document[] counters = java.util.Arrays.stream(values)
            .map(value -> new Document("value", value))
            .toArray(Document[]::new);
        when(mongoOperations.findAndModify(
            any(Query.class),
            any(UpdateDefinition.class),
            any(FindAndModifyOptions.class),
            eq(Document.class),
            eq("counters")
        )).thenReturn(counters[0], java.util.Arrays.copyOfRange(counters, 1, counters.length));
    }
}
