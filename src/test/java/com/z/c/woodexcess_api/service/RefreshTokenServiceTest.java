package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.TokenRotationResult;
import com.z.c.woodexcess_api.exception.auth.RefreshTokenException;
import com.z.c.woodexcess_api.exception.auth.TokenReuseDetectedException;
import com.z.c.woodexcess_api.model.RefreshToken;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.RefreshTokenRepository;
import com.z.c.woodexcess_api.role.UserRole;
import com.z.c.woodexcess_api.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Complete Unit Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;
    private RefreshToken refreshToken;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String RAW_TOKEN = "test-refresh-token-uuid";
    private static final String TOKEN_HASH = "5a2f8e5c4d3b2a1f8e9d7c6b5a4f3e2d1c0b9a8f7e6d5c4b3a2f1e0d9c8b7a6f";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .email("test@mail.com")
                .name("Test User")
                .role(UserRole.USER)
                .active(true)
                .build();

        refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(TOKEN_HASH)
                .userAgent(USER_AGENT)
                .ipAddress(IP_ADDRESS)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        ReflectionTestUtils.setField(refreshTokenService, "maxDevices", 5);
        ReflectionTestUtils.setField(refreshTokenService, "rotationEnabled", true);
        ReflectionTestUtils.setField(refreshTokenService, "validateContext", true);
        ReflectionTestUtils.setField(refreshTokenService, "reuseDetectionEnabled", true);
    }

    @Test
    @DisplayName("Should create refresh token successfully")
    void shouldCreateRefreshTokenSuccessfully() {
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
        when(repository.countActiveTokensByUserId(eq(USER_ID), any(LocalDateTime.class))).thenReturn(3L);
        when(jwtProvider.generateRefreshToken()).thenReturn(RAW_TOKEN);
        when(jwtProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        String result = refreshTokenService.createRefreshToken(user, request);

        assertThat(result).isEqualTo(RAW_TOKEN);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getUserAgent()).isEqualTo(USER_AGENT);
        assertThat(savedToken.getIpAddress()).isEqualTo(IP_ADDRESS);
    }

    @Test
    @DisplayName("Should enforce device limit when max devices reached")
    void shouldEnforceDeviceLimitWhenMaxReached() {
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
        when(repository.countActiveTokensByUserId(eq(USER_ID), any(LocalDateTime.class))).thenReturn(5L);

        List<RefreshToken> tokens = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("hash" + i)
                    .createdAt(LocalDateTime.now().minusDays(i))
                    .revoked(false)
                    .build();
            tokens.add(token);
        }

        when(repository.findActiveTokensByUserId(USER_ID)).thenReturn(tokens);
        when(jwtProvider.generateRefreshToken()).thenReturn(RAW_TOKEN);
        when(jwtProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        String result = refreshTokenService.createRefreshToken(user, request);

        assertThat(result).isEqualTo(RAW_TOKEN);
        verify(repository, atLeastOnce()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should extract IP from X-Forwarded-For header")
    void shouldExtractIpFromXForwardedForHeader() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(repository.countActiveTokensByUserId(eq(USER_ID), any(LocalDateTime.class))).thenReturn(0L);
        when(jwtProvider.generateRefreshToken()).thenReturn(RAW_TOKEN);
        when(jwtProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        refreshTokenService.createRefreshToken(user, request);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("Should validate and rotate token successfully")
    void shouldValidateAndRotateTokenSuccessfully() {
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
        String newToken = "new-refresh-token-uuid";
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
        when(jwtProvider.generateRefreshToken()).thenReturn(newToken);
        when(jwtProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        TokenRotationResult result = refreshTokenService.validateAndRotate(RAW_TOKEN, request);

        assertThat(result).isNotNull();
        assertThat(result.getRawToken()).isEqualTo(newToken);
        assertThat(result.getUserId()).isEqualTo(USER_ID);

        verify(repository, times(2)).save(any(RefreshToken.class));
        assertThat(refreshToken.getRevoked()).isTrue();
        assertThat(refreshToken.getReplacedByToken()).isNotNull();
    }

    @Test
    @DisplayName("Should throw RefreshTokenException when token not found")
    void shouldThrowExceptionWhenTokenNotFound() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(RAW_TOKEN, request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Invalid refresh token");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw TokenReuseDetectedException when token was replaced")
    void shouldThrowExceptionWhenTokenWasReplaced() {
        refreshToken.setReplacedByToken("some-new-token-hash");
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(RAW_TOKEN, request))
                .isInstanceOf(TokenReuseDetectedException.class)
                .hasMessage("Token reuse detected. All sessions revoked.");

        verify(repository).revokeAllByUserId(USER_ID);
    }

    @Test
    @DisplayName("Should throw RefreshTokenException when token expired")
    void shouldThrowExceptionWhenTokenExpired() {
        refreshToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(RAW_TOKEN, request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Refresh token has expired");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw RefreshTokenException when token revoked")
    void shouldThrowExceptionWhenTokenRevoked() {
        refreshToken.setRevoked(true);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(RAW_TOKEN, request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Refresh token has been revoked");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw RefreshTokenException when context mismatch - User-Agent")
    void shouldThrowExceptionWhenUserAgentMismatch() {
        when(request.getHeader("User-Agent")).thenReturn("Different-User-Agent");
        when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(RAW_TOKEN, request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Token context mismatch. Please login again.");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw RefreshTokenException when context mismatch - IP Address")
    void shouldThrowExceptionWhenIpAddressMismatch() {
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(RAW_TOKEN, request))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessage("Token context mismatch. Please login again.");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should not rotate token when rotation disabled")
    void shouldNotRotateTokenWhenRotationDisabled() {
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
        ReflectionTestUtils.setField(refreshTokenService, "rotationEnabled", false);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        TokenRotationResult result = refreshTokenService.validateAndRotate(RAW_TOKEN, request);

        assertThat(result).isNotNull();
        assertThat(result.getRawToken()).isEqualTo(RAW_TOKEN);
        assertThat(result.getUserId()).isEqualTo(USER_ID);

        verify(repository, times(1)).save(refreshToken);
        assertThat(refreshToken.getRevoked()).isFalse();
        assertThat(refreshToken.getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not validate context when validation disabled")
    void shouldNotValidateContextWhenDisabled() {
        // Desabilitar validação de contexto
        ReflectionTestUtils.setField(refreshTokenService, "validateContext", false);

        // Setup apenas os mocks necessários
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
        when(jwtProvider.generateRefreshToken()).thenReturn("new-token");
        when(jwtProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        TokenRotationResult result = refreshTokenService.validateAndRotate(RAW_TOKEN, request);

        // Then
        assertThat(result).isNotNull();
        verify(repository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should revoke token successfully")
    void shouldRevokeTokenSuccessfully() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        refreshTokenService.revokeToken(RAW_TOKEN);

        assertThat(refreshToken.getRevoked()).isTrue();
        verify(repository).save(refreshToken);
    }

    @Test
    @DisplayName("Should handle revoke when token not found")
    void shouldHandleRevokeWhenTokenNotFound() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        refreshTokenService.revokeToken(RAW_TOKEN);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should revoke all user tokens")
    void shouldRevokeAllUserTokens() {
        doNothing().when(repository).revokeAllByUserId(USER_ID);

        refreshTokenService.revokeAllUserTokens(USER_ID);

        verify(repository).revokeAllByUserId(USER_ID);
    }

    @Test
    @DisplayName("Should cleanup expired tokens")
    void shouldCleanupExpiredTokens() {
        doNothing().when(repository).deleteExpiredAndRevokedTokens(any(LocalDateTime.class));

        refreshTokenService.cleanupExpiredTokens();

        verify(repository).deleteExpiredAndRevokedTokens(any(LocalDateTime.class));
    }
}