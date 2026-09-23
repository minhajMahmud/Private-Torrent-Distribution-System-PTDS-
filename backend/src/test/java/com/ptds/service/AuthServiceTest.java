package com.ptds.service;

import com.ptds.dto.LoginRequest;
import com.ptds.dto.RegisterRequest;
import com.ptds.entity.Role;
import com.ptds.entity.User;
import com.ptds.exception.BadCredentialsCustomException;
import com.ptds.exception.DuplicateResourceException;
import com.ptds.repository.RoleRepository;
import com.ptds.repository.UserRepository;
import com.ptds.repository.VerificationTokenRepository;
import com.ptds.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "verificationExpiryHours", 24L);
        ReflectionTestUtils.setField(authService, "resetExpiryMinutes", 30L);
    }

    @Test
    void register_throwsWhenUsernameTaken() {
        RegisterRequest req = RegisterRequest.builder()
                .username("existinguser").email("new@example.com").password("Passw0rd!").build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throwsWhenEmailTaken() {
        RegisterRequest req = RegisterRequest.builder()
                .username("newuser").email("taken@example.com").password("Passw0rd!").build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void register_savesUserAndSendsVerificationEmail() {
        RegisterRequest req = RegisterRequest.builder()
                .username("newuser").email("new@example.com").password("Passw0rd!").fullName("New User").build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(Role.builder().id(1L).name("ROLE_USER").build()));
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("hashed");

        authService.register(req);

        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(eq("new@example.com"), any());
        verify(auditLogService).log(any(), eq("USER_REGISTER"), eq("User"), any());
    }

    @Test
    void login_throwsOnBadCredentials() {
        LoginRequest req = LoginRequest.builder().usernameOrEmail("user1").password("wrongpass").build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .email("user1@example.com")
                .passwordHash("hashed")
                .emailVerified(true)
                .roles(Set.of(Role.builder().id(1L).name("ROLE_USER").build()))
                .build();

        when(userRepository.findByUsernameOrEmail("user1", "user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsCustomException.class);
    }

    @Test
    void login_throwsWhenEmailNotVerified() {
        LoginRequest req = LoginRequest.builder().usernameOrEmail("user1").password("Passw0rd!").build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .email("user1@example.com")
                .passwordHash("hashed")
                .emailVerified(false)
                .roles(Set.of(Role.builder().id(1L).name("ROLE_USER").build()))
                .build();

        when(userRepository.findByUsernameOrEmail("user1", "user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Passw0rd!", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsCustomException.class)
                .hasMessageContaining("verify");
    }

    @Test
    void login_succeedsAndReturnsTokens() {
        LoginRequest req = LoginRequest.builder().usernameOrEmail("user1").password("Passw0rd!").build();
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .username("user1")
                .email("user1@example.com")
                .passwordHash("hashed")
                .emailVerified(true)
                .roles(Set.of(Role.builder().id(1L).name("ROLE_USER").build()))
                .build();

        when(userRepository.findByUsernameOrEmail("user1", "user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Passw0rd!", "hashed")).thenReturn(true);
        when(jwtUtil.generateAccessToken(eq(id), eq("user1"), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(id, "user1")).thenReturn("refresh-token");

        var response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("user1");
    }
}
