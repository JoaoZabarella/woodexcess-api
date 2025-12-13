package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.TokenRotationResult;
import com.z.c.woodexcess_api.enums.UserRole;
import com.z.c.woodexcess_api.exception.auth.RefreshTokenException;
import com.z.c.woodexcess_api.model.RefreshToken;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.RefreshTokenRepository;
import com.z.c.woodexcess_api.security.JwtProvider;
import com.z.c.woodexcess_api.service.security.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for RefreshTokenService.
 *
 * Test Strategy:
 * - Uses Given-When-Then pattern
 * - Tests behavior, not implementation
 * - Covers happy paths and edge cases
 * - Uses nested classes for logical grouping
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private RefreshTokenService service;

    // Test Data Builders
    private TestDataBuilder testData;

    @BeforeEach
    void setUp() {
        testData = new TestDataBuilder();
        configureDefaultBehavior();
    }

    private void configureDefaultBehavior() {
        // Default service configuration
        ReflectionTestUtils.setField(service, "maxDevices", 5);
        ReflectionTestUtils.setField(service, "rotationEnabled", true);
        ReflectionTestUtils.setField(service, "validateContext", true);
        ReflectionTestUtils.setField(service, "reuseDetectionEnabled", true);


    }


    @Nested
    @DisplayName("createRefreshToken()")
    class CreateRefreshTokenTests {

        @Test
        @DisplayName("Should create token with all metadata when request is valid")
        void shouldCreateTokenWithAllMetadata() {
            // Given
            User user = testData.createUser();
            String expectedToken = "generated-refresh-token";

            when(request.getHeader("User-Agent")).thenReturn(TestDataBuilder.USER_AGENT);
            when(request.getRemoteAddr()).thenReturn(TestDataBuilder.IP_ADDRESS);
            when(repository.countActiveTokensByUserId(eq(user.getId()), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(jwtProvider.generateRefreshToken()).thenReturn(expectedToken);
            when(jwtProvider.getRefreshTokenExpiration()).thenReturn(604800000L);

            // When
            String result = service.createRefreshToken(user, request);

            // Then
            assertThat(result).isEqualTo(expectedToken);

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository).save(tokenCaptor.capture());

            RefreshToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken)
                    .satisfies(token -> {
                        assertThat(token.getUser()).isEqualTo(user);
                        assertThat(token.getUserAgent()).isEqualTo(TestDataBuilder.USER_AGENT);
                        assertThat(token.getIpAddress()).isEqualTo(TestDataBuilder.IP_ADDRESS);
                        assertThat(token.getRevoked()).isFalse();
                        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
                        assertThat(token.getTokenHash()).isNotEmpty();
                    });
        }

        @Test
        @DisplayName("Should extract IP from X-Forwarded-For header when available")
        void shouldExtractIpFromXForwardedForHeader() {
            // Given
            User user = testData.createUser();
            String realIp = "192.168.1.100";

            when(request.getHeader("X-Forwarded-For")).thenReturn(realIp);
            when(request.getHeader("User-Agent")).thenReturn(TestDataBuilder.USER_AGENT);
            when(repository.countActiveTokensByUserId(any(), any())).thenReturn(0L);
            when(jwtProvider.generateRefreshToken()).thenReturn("token");

            // When
            service.createRefreshToken(user, request);

            // Then
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo(realIp);
        }

        @Test
        @DisplayName("Should handle missing User-Agent gracefully")
        void shouldHandleMissingUserAgent() {
            // Given
            User user = testData.createUser();

            when(request.getHeader("User-Agent")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn(TestDataBuilder.IP_ADDRESS);
            when(repository.countActiveTokensByUserId(any(), any())).thenReturn(0L);
            when(jwtProvider.generateRefreshToken()).thenReturn("token");
            when(jwtProvider.getRefreshTokenExpiration()).thenReturn(604800000L);

            // When
            service.createRefreshToken(user, request);

            // Then
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getUserAgent()).isEqualTo("Unknown");
        }

        @Test
        @DisplayName("Should revoke oldest token when device limit is reached")
        void shouldRevokeOldestTokenWhenLimitReached() {
            // Given
            User user = testData.createUser();
            List<RefreshToken> existingTokens = testData.createActiveTokens(user, 5);

            when(request.getHeader("User-Agent")).thenReturn(TestDataBuilder.USER_AGENT);
            when(request.getRemoteAddr()).thenReturn(TestDataBuilder.IP_ADDRESS);
            when(repository.countActiveTokensByUserId(eq(user.getId()), any(LocalDateTime.class)))
                    .thenReturn(5L);
            when(repository.findActiveTokensByUserId(user.getId()))
                    .thenReturn(existingTokens);
            when(jwtProvider.generateRefreshToken()).thenReturn("new-token");

            // When
            service.createRefreshToken(user, request);

            // Then
            RefreshToken oldestToken = existingTokens.get(0);
            assertThat(oldestToken.getRevoked()).isTrue();
            verify(repository, times(2)).save(any(RefreshToken.class));
            // 1x revoke oldest, 1x save new
        }

        @Test
        @DisplayName("Should allow multiple tokens when under device limit")
        void shouldAllowMultipleTokensUnderLimit() {
            // Given
            User user = testData.createUser();

            when(request.getHeader("User-Agent")).thenReturn(TestDataBuilder.USER_AGENT);
            when(request.getRemoteAddr()).thenReturn(TestDataBuilder.IP_ADDRESS);
            when(repository.countActiveTokensByUserId(eq(user.getId()), any(LocalDateTime.class)))
                    .thenReturn(3L);
            when(jwtProvider.generateRefreshToken()).thenReturn("token");

            // When
            service.createRefreshToken(user, request);

            // Then
            verify(repository, times(1)).save(any(RefreshToken.class));
            verify(repository, never()).findActiveTokensByUserId(any());
        }
    }



    @Nested
    @DisplayName("validateAndRotate()")
    class ValidateAndRotateTests {

        @Test
        @DisplayName("Should rotate valid token successfully")
        void shouldRotateValidToken() {
            // Given
            User user = testData.createUser();
            RefreshToken oldToken = testData.createValidToken(user);
            String rawToken = "valid-token";
            String newRawToken = "new-token";

            when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(oldToken));
            when(request.getHeader("User-Agent")).thenReturn(oldToken.getUserAgent());
            when(request.getRemoteAddr()).thenReturn(oldToken.getIpAddress());
            when(jwtProvider.generateRefreshToken()).thenReturn(newRawToken);

            // When
            TokenRotationResult result = service.validateAndRotate(rawToken, request);

            // Then
            assertThat(result)
                    .satisfies(r -> {
                        assertThat(r.rawToken()).isEqualTo(newRawToken);
                        assertThat(r.userId()).isEqualTo(user.getId());
                    });

            assertThat(oldToken.getRevoked()).isTrue();
            verify(repository, times(2)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Given
            User user = testData.createUser();
            RefreshToken expiredToken = testData.createExpiredToken(user);

            when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

            // When / Then
            assertThatThrownBy(() -> service.validateAndRotate("expired-token", request))
                    .isInstanceOf(RefreshTokenException.class)
                    .hasMessageContaining("expired");

            verify(repository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should reject revoked token")
        void shouldRejectRevokedToken() {
            // Given
            User user = testData.createUser();
            RefreshToken revokedToken = testData.createRevokedToken(user);

            when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

            // When / Then
            assertThatThrownBy(() -> service.validateAndRotate("revoked-token", request))
                    .isInstanceOf(RefreshTokenException.class)
                    .hasMessageContaining("revoked");

            verify(repository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should reject non-existent token")
        void shouldRejectNonExistentToken() {
            // Given
            when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.validateAndRotate("invalid-token", request))
                    .isInstanceOf(RefreshTokenException.class)
                    .hasMessageContaining("Invalid");
        }

        @Test
        @DisplayName("Should detect token reuse attempt")
        void shouldDetectTokenReuseAttempt() {
            // Given
            User user = testData.createUser();
            RefreshToken reusedToken = testData.createValidToken(user);
            reusedToken.setRevoked(true); // Simula token já rotacionado
            reusedToken.setLastUsedAt(LocalDateTime.now().minusMinutes(5));

            when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(reusedToken));

            // When / Then
            assertThatThrownBy(() -> service.validateAndRotate("reused-token", request))
                    .isInstanceOf(RefreshTokenException.class);
        }

        @Test
        @DisplayName("Should reject token from different IP when context validation enabled")
        void shouldRejectTokenFromDifferentIp() {
            // Given
            User user = testData.createUser();
            RefreshToken token = testData.createValidToken(user);
            token.setIpAddress("192.168.1.1");

            when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
            when(request.getRemoteAddr()).thenReturn("10.0.0.1"); // Different IP
            when(request.getHeader("User-Agent")).thenReturn(token.getUserAgent());

            // When / Then
            assertThatThrownBy(() -> service.validateAndRotate("token", request))
                    .isInstanceOf(RefreshTokenException.class)
                    .hasMessageContaining("context");
        }

        @Test
        @DisplayName("Should allow rotation when context validation disabled")
        void shouldAllowRotationWhenContextValidationDisabled() {
            // Given
            ReflectionTestUtils.setField(service, "validateContext", false);

            User user = testData.createUser();
            RefreshToken token = testData.createValidToken(user);
            token.setIpAddress("192.168.1.1");

            when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
            when(jwtProvider.generateRefreshToken()).thenReturn("new-token");

            // When
            TokenRotationResult result = service.validateAndRotate("token", request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.rawToken()).isEqualTo("new-token");
        }
    }


    @Nested
    @DisplayName("revokeToken()")
    class RevokeTokenTests {

        @Test
        @DisplayName("Should revoke token when found")
        void shouldRevokeTokenWhenFound() {
            // Given
            User user = testData.createUser();
            RefreshToken token = testData.createValidToken(user);

            when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            // When
            service.revokeToken("valid-token");

            // Then
            assertThat(token.getRevoked()).isTrue();
            verify(repository).save(token);
        }

        @Test
        @DisplayName("Should handle gracefully when token not found")
        void shouldHandleGracefullyWhenTokenNotFound() {
            // Given
            when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            // When
            service.revokeToken("non-existent-token");

            // Then
            verify(repository, never()).save(any());
            // No exception should be thrown
        }

        @Test
        @DisplayName("Should be idempotent when revoking already revoked token")
        void shouldBeIdempotentWhenRevokingAlreadyRevokedToken() {
            // Given
            User user = testData.createUser();
            RefreshToken token = testData.createRevokedToken(user);

            when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            // When
            service.revokeToken("already-revoked");

            // Then
            assertThat(token.getRevoked()).isTrue();
            verify(repository).save(token); // Still saves but no state change
        }
    }


    @Nested
    @DisplayName("revokeAllUserTokens()")
    class RevokeAllUserTokensTests {

        @Test
        @DisplayName("Should revoke all tokens for user")
        void shouldRevokeAllTokensForUser() {
            // Given
            UUID userId = UUID.randomUUID();
            doNothing().when(repository).revokeAllByUserId(userId);

            // When
            service.revokeAllUserTokens(userId);

            // Then
            verify(repository).revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("Should handle when user has no tokens")
        void shouldHandleWhenUserHasNoTokens() {
            // Given
            UUID userId = UUID.randomUUID();
            doNothing().when(repository).revokeAllByUserId(userId);

            // When
            service.revokeAllUserTokens(userId);

            // Then
            verify(repository).revokeAllByUserId(userId);
            // Should complete without errors
        }
    }



    @Nested
    @DisplayName("cleanupExpiredTokens()")
    class CleanupExpiredTokensTests {

        @Test
        @DisplayName("Should delete expired and revoked tokens")
        void shouldDeleteExpiredAndRevokedTokens() {
            // Given
            doNothing().when(repository).deleteExpiredAndRevokedTokens(any(LocalDateTime.class));

            // When
            service.cleanupExpiredTokens();

            // Then
            verify(repository).deleteExpiredAndRevokedTokens(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should use current time as cutoff for cleanup")
        void shouldUseCurrentTimeAsCutoffForCleanup() {
            // Given
            ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            doNothing().when(repository).deleteExpiredAndRevokedTokens(timeCaptor.capture());

            LocalDateTime beforeCleanup = LocalDateTime.now();

            // When
            service.cleanupExpiredTokens();

            // Then
            LocalDateTime afterCleanup = LocalDateTime.now();
            LocalDateTime capturedTime = timeCaptor.getValue();

            assertThat(capturedTime)
                    .isAfterOrEqualTo(beforeCleanup)
                    .isBeforeOrEqualTo(afterCleanup);
        }
    }

    private static class TestDataBuilder {
        static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
        static final String IP_ADDRESS = "127.0.0.1";
        static final String TOKEN_HASH = "5a2f8e5c4d3b2a1f8e9d7c6b5a4f3e2d1c0b9a8f7e6d5c4b3a2f1e0d9c8b7a6f";

        User createUser() {
            return User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .name("Test User")
                    .role(UserRole.USER)
                    .isActive(true)
                    .build();
        }

        RefreshToken createValidToken(User user) {
            return RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash(TOKEN_HASH)
                    .userAgent(USER_AGENT)
                    .ipAddress(IP_ADDRESS)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        RefreshToken createExpiredToken(User user) {
            RefreshToken token = createValidToken(user);
            token.setExpiresAt(LocalDateTime.now().minusDays(1));
            return token;
        }

        RefreshToken createRevokedToken(User user) {
            RefreshToken token = createValidToken(user);
            token.setRevoked(true);
            return token;
        }

        List<RefreshToken> createActiveTokens(User user, int count) {
            return IntStream.range(0, count)
                    .mapToObj(i -> {
                        RefreshToken token = createValidToken(user);
                        token.setCreatedAt(LocalDateTime.now().minusDays(count - i));
                        return token;
                    })
                    .toList();
        }
    }
}
