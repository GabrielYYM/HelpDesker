package com.corp.DeskHelper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.corp.DeskHelper.dto.UserCreateRequestDTO;
import com.corp.DeskHelper.model.User;
import com.corp.DeskHelper.model.enums.UserRole;
import com.corp.DeskHelper.repository.UserRepository;

class UserServiceTests {

    private final UserRepository repository = mock(UserRepository.class);
    private final UserService service = new UserService(repository);

    @Test
    void firstUserStartsAtCode0001WithDefaultRoleAndPendingFirstAccess() {
        when(repository.findFirstByOrderByCodeDesc()).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertThat(user.getPassword()).isNull();
            return user;
        });

        var created = service.createUser(new UserCreateRequestDTO("  Pessoa de teste  ", null));

        assertThat(created.code()).isEqualTo("0001");
        assertThat(created.name()).isEqualTo("Pessoa de teste");
        assertThat(created.role()).isEqualTo(UserRole.USER);
        assertThat(created.active()).isTrue();
        assertThat(created.firstAccess()).isTrue();
    }

    @Test
    void createUsesNextCodeAndKeepsRequestedRole() {
        when(repository.findFirstByOrderByCodeDesc()).thenReturn(Optional.of(User.builder().code("0042").build()));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.createUser(new UserCreateRequestDTO("Equipe", UserRole.IT));

        assertThat(created.code()).isEqualTo("0043");
        assertThat(created.role()).isEqualTo(UserRole.IT);
    }
}
