package com.ptds.service;

import com.ptds.dto.*;
import com.ptds.entity.Role;
import com.ptds.entity.User;
import com.ptds.entity.VerificationToken;
import com.ptds.exception.BadCredentialsCustomException;
import com.ptds.exception.DuplicateResourceException;
import com.ptds.exception.InvalidTokenException;
import com.ptds.exception.ResourceNotFoundException;
import com.ptds.repository.RoleRepository;
import com.ptds.repository.UserRepository;
import com.ptds.repository.VerificationTokenRepository;
import com.ptds.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Value("${app.email.verification-expiry-hours}")
    private long verificationExpiryHours;

    @Value("${app.email.password-reset-expiry-minutes}")
    private long resetExpiryMinutes;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER.name())
                .orElseThrow(() -> new ResourceNotFoundException("Default role not seeded"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .emailVerified(false)
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);

        String token = generateSecureToken();
        VerificationToken verificationToken = VerificationToken.builder()
                .user(user)
                .token(token)
                .tokenType(VerificationToken.TokenType.EMAIL_VERIFY)
                .expiresAt(OffsetDateTime.now().plusHours(verificationExpiryHours))
                .build();
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), token);
        auditLogService.log(user.getId(), "USER_REGISTER", "User", user.getId().toString());
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken vt = tokenRepository.findByTokenAndTokenType(token, VerificationToken.TokenType.EMAIL_VERIFY)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (vt.isUsed()) throw new InvalidTokenException("Token already used");
        if (vt.isExpired()) throw new InvalidTokenException("Token has expired, please request a new one");

        User user = vt.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        vt.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(vt);

        auditLogService.log(user.getId(), "EMAIL_VERIFIED", "User", user.getId().toString());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new BadCredentialsCustomException("Invalid username/email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsCustomException("Invalid username/email or password");
        }
        if (user.isLocked()) {
            throw new BadCredentialsCustomException("Account is locked, contact an administrator");
        }
        if (!user.isEmailVerified()) {
            throw new BadCredentialsCustomException("Please verify your email before logging in");
        }

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String access = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refresh = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        auditLogService.log(user.getId(), "USER_LOGIN", "User", user.getId().toString());

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(Set.copyOf(roles))
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken) || !"REFRESH".equals(jwtUtil.extractTokenType(refreshToken))) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
        String username = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String access = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles);

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(Set.copyOf(roles))
                .accessToken(access)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = generateSecureToken();
            VerificationToken vt = VerificationToken.builder()
                    .user(user)
                    .token(token)
                    .tokenType(VerificationToken.TokenType.PASSWORD_RESET)
                    .expiresAt(OffsetDateTime.now().plusMinutes(resetExpiryMinutes))
                    .build();
            tokenRepository.save(vt);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            auditLogService.log(user.getId(), "PASSWORD_RESET_REQUESTED", "User", user.getId().toString());
        });
        // Intentionally do not reveal whether the email exists (prevents user enumeration)
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        VerificationToken vt = tokenRepository.findByTokenAndTokenType(token, VerificationToken.TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        if (vt.isUsed()) throw new InvalidTokenException("Token already used");
        if (vt.isExpired()) throw new InvalidTokenException("Token has expired, please request a new one");

        User user = vt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        vt.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(vt);

        auditLogService.log(user.getId(), "PASSWORD_RESET_COMPLETED", "User", user.getId().toString());
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsCustomException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.log(user.getId(), "PASSWORD_CHANGED", "User", user.getId().toString());
    }

    private String generateSecureToken() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }
}
