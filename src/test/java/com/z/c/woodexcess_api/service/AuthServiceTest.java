package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.LoginResponse;
import com.z.c.woodexcess_api.dto.auth.TokenRotationResult;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.role.UserRole;
import com.z.c.woodexcess_api.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private static final String VALID_EMAIL = "test@mail.com";
    private static final String VALID_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPassword";

    @BeforeEach
    void setUp() {
        // Configurar accessTokenExpiration via reflexão
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 900000L);

        // Usuário ativo padrão
        activeUser = User.builder()
                .id(UUID.randomUUID())
                .email(VALID_EMAIL)
                .password(ENCODED_PASSWORD)
                .name("Test User")
                .role(UserRole.USER)
                .active(true)
                .build();
    }

// ========== TESTES DE authenticate() ==========

    @Test
    @DisplayName("Should authenticate successfully with valid credentials")
    void shouldAuthenticateSuccessfully() {
        // Given
        String accessToken = "valid.access.token";
        String refreshToken = "valid-refresh-token";

        when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(VALID_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtProvider.generateJwtToken(activeUser)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(activeUser, request)).thenReturn(refreshToken);

        // When
        LoginResponse response = authService.authenticate(VALID_EMAIL, VALID_PASSWORD, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
        assertThat(response.expiresIn()).isEqualTo(900000L);

        verify(userRepository).findByEmail(VALID_EMAIL);
        verify(passwordEncoder).matches(VALID_PASSWORD, ENCODED_PASSWORD);
        verify(jwtProvider).generateJwtToken(activeUser);
        verify(refreshTokenService).createRefreshToken(activeUser, request);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when email not found")
    void shouldThrowExceptionWhenEmailNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(VALID_EMAIL, VALID_PASSWORD, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(userRepository).findByEmail(VALID_EMAIL);
        verifyNoInteractions(passwordEncoder, jwtProvider, refreshTokenService);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when password is wrong")
    void shouldThrowExceptionWhenPasswordIsWrong() {
        // Given
        when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(VALID_EMAIL, "wrongPassword", request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(userRepository).findByEmail(VALID_EMAIL);
        verify(passwordEncoder).matches("wrongPassword", ENCODED_PASSWORD);
        verifyNoInteractions(jwtProvider, refreshTokenService);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user is inactive")
    void shouldThrowExceptionWhenUserIsInactive() {
        // Given
        activeUser.setActive(false);
        when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(VALID_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(VALID_EMAIL, VALID_PASSWORD, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(userRepository).findByEmail(VALID_EMAIL);
        verify(passwordEncoder).matches(VALID_PASSWORD, ENCODED_PASSWORD);
        verifyNoInteractions(jwtProvider, refreshTokenService);
    }

// ========== TESTES DE refreshAccessToken() ==========

    @Test
    @DisplayName("Should refresh access token successfully")
    void shouldRefreshAccessTokenSuccessfully() {
        // Given
        String oldRefreshToken = "old-refresh-token";
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new-refresh-token";
        UUID userId = activeUser.getId();

        TokenRotationResult rotationResult = TokenRotationResult.builder()
                .rawToken(newRefreshToken)
                .tokenHash("hash123")
                .userId(userId)
                .build();

        when(refreshTokenService.validateAndRotate(oldRefreshToken, request))
                .thenReturn(rotationResult);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(jwtProvider.generateJwtToken(activeUser)).thenReturn(newAccessToken);

        // When
        LoginResponse response = authService.refreshAccessToken(oldRefreshToken, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(newAccessToken);
        assertThat(response.refreshToken()).isEqualTo(newRefreshToken);
        assertThat(response.expiresIn()).isEqualTo(900000L);

        verify(refreshTokenService).validateAndRotate(oldRefreshToken, request);
        verify(userRepository).findById(userId);
        verify(jwtProvider).generateJwtToken(activeUser);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user not found during refresh")
    void shouldThrowExceptionWhenUserNotFoundDuringRefresh() {
        // Given
        String refreshToken = "valid-refresh-token";
        UUID userId = UUID.randomUUID();

        TokenRotationResult rotationResult = TokenRotationResult.builder()
                .rawToken("new-token")
                .tokenHash("hash")
                .userId(userId)
                .build();

        when(refreshTokenService.validateAndRotate(refreshToken, request))
                .thenReturn(rotationResult);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("User not found");

        verify(refreshTokenService).validateAndRotate(refreshToken, request);
        verify(userRepository).findById(userId);
        verifyNoInteractions(jwtProvider);
    }

// ========== TESTES DE logout() ==========

    @Test
    @DisplayName("Should logout and revoke refresh token")
    void shouldLogoutSuccessfully() {
        // Given
        String refreshToken = "refresh-token-to-revoke";
        doNothing().when(refreshTokenService).revokeToken(refreshToken);

        // When
        authService.logout(refreshToken);

        // Then
        verify(refreshTokenService).revokeToken(refreshToken);
    }

    @Test
    @DisplayName("Should handle logout even if token is invalid")
    void shouldHandleLogoutWithInvalidToken() {
        // Given
        String invalidToken = "invalid-token";
        doNothing().when(refreshTokenService).revokeToken(invalidToken);

        // When
        authService.logout(invalidToken);

        // Then
        verify(refreshTokenService).revokeToken(invalidToken);
    }

}