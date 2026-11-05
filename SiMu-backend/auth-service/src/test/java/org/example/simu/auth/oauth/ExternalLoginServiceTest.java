package org.example.simu.auth.oauth;

import org.example.simu.auth.entity.User;
import org.example.simu.auth.entity.UserIdentity;
import org.example.simu.auth.exception.EmailNotVerifiedException;
import org.example.simu.auth.repository.UserIdentityRepository;
import org.example.simu.auth.repository.UserRepository;
import org.example.simu.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalLoginServiceTest {

    private UserIdentityRepository identities;
    private UserRepository users;
    private UserService userService;
    private ExternalLoginService service;

    @BeforeEach
    void setUp() {
        identities = mock(UserIdentityRepository.class);
        users = mock(UserRepository.class);
        userService = mock(UserService.class);
        service = new ExternalLoginService(identities, users, userService);
    }

    @Test
    void knownSubjectReturnsTheLinkedAccount() {
        UserIdentity identity = new UserIdentity();
        identity.setUserId(7L);
        when(identities.findByProviderAndSubject("google", "sub-1")).thenReturn(Optional.of(identity));
        when(users.findById(7L)).thenReturn(Optional.of(user(7L)));

        User result = service.findOrCreate(new GoogleIdentity("sub-1", "a@example.com", true, "A"));

        assertThat(result.getId()).isEqualTo(7L);
        verify(userService, never()).createExternalUser(anyString(), anyString());
    }

    @Test
    void unverifiedEmailIsNeverLinked() {
        when(identities.findByProviderAndSubject("google", "sub-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOrCreate(new GoogleIdentity("sub-2", "victim@example.com", false, "V")))
                .isInstanceOf(EmailNotVerifiedException.class);

        verify(identities, never()).save(any());
        verify(userService, never()).createExternalUser(anyString(), anyString());
    }

    @Test
    void verifiedEmailIsLinkedToTheExistingAccountInsteadOfCreatingANewOne() {
        when(identities.findByProviderAndSubject("google", "sub-3")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("b@example.com")).thenReturn(Optional.of(user(11L)));

        User result = service.findOrCreate(new GoogleIdentity("sub-3", "b@example.com", true, "B"));

        assertThat(result.getId()).isEqualTo(11L);
        verify(userService, never()).createExternalUser(anyString(), anyString());

        ArgumentCaptor<UserIdentity> saved = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identities).save(saved.capture());
        assertThat(saved.getValue().getProvider()).isEqualTo("google");
        assertThat(saved.getValue().getSubject()).isEqualTo("sub-3");
        assertThat(saved.getValue().getUserId()).isEqualTo(11L);
    }

    @Test
    void unknownSubjectWithVerifiedEmailCreatesAPasswordlessAccount() {
        when(identities.findByProviderAndSubject("google", "sub-4")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("c@example.com")).thenReturn(Optional.empty());
        when(users.existsByUsername(anyString())).thenReturn(false);
        when(userService.createExternalUser(anyString(), eq("c@example.com"))).thenReturn(user(21L));

        User result = service.findOrCreate(new GoogleIdentity("sub-4", "c@example.com", true, "C"));

        assertThat(result.getId()).isEqualTo(21L);
        verify(identities).save(any(UserIdentity.class));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setEmail("user" + id + "@example.com");
        user.setRoles(java.util.Set.of("USER"));
        return user;
    }
}