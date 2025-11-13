package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.exception.auth.RefreshTokenException;
import com.z.c.woodexcess_api.exception.auth.TokenReuseDetectedException;
import com.z.c.woodexcess_api.model.RefreshToken;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.RefreshTokenRepository;
import com.z.c.woodexcess_api.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final JwtProvider jwtProvider;

    @Value("${security.refresh-token.max-devices:5}")
    private int maxDevices;

    @Value("${security.refresh-token.rotation-enabled:true}")
    private boolean rotationEnabled;

    @Value("${security.refresh-token.validate-context:true}")
    private boolean validateContext;

    @Value("${security.refresh-token.reuse-detection:true}")
    private boolean reuseDetectionEnabled;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProvider jwtProvider) {
        this.repository = repository;
        this.jwtProvider = jwtProvider;
    }


     // Cria um novo refresh token e retorna o TOKEN ORIGINAL (não o hash)
    @Transactional
    public String createRefreshToken(User user, HttpServletRequest request) {
        // Limitar dispositivos por usuário
        enforceDeviceLimit(user.getId());

        // Gerar token original
        String token = jwtProvider.generateRefreshToken();
        String tokenHash = hashToken(token);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(getClientIp(request))
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenExpiration() / 1000))
                .build();

        repository.save(refreshToken);

        logger.info("Refresh token created for user: {} from IP: {}", user.getEmail(), refreshToken.getIpAddress());
        return token;
    }

    @Transactional
    public RefreshToken validateAndRotate(String token, HttpServletRequest request) {
        String tokenHash = hashToken(token);

        RefreshToken currentToken = repository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RefreshTokenException("Invalid refresh token"));

        // ✅ Detecção de reuso: Se token já foi substituído, possível ataque
        if (reuseDetectionEnabled && currentToken.getReplacedByToken() != null) {
            logger.warn("Token reuse detected for user: {}. Revoking all tokens.",
                    currentToken.getUser().getEmail());
            revokeAllUserTokens(currentToken.getUser().getId());
            throw new TokenReuseDetectedException("Token reuse detected. All sessions revoked.");
        }

        // Validar expiração
        if (currentToken.isExpired()) {
            throw new RefreshTokenException("Refresh token has expired");
        }

        // Validar revogação
        if (currentToken.getRevoked()) {
            throw new RefreshTokenException("Refresh token has been revoked");
        }

        // ✅ Validação de contexto (IP + User-Agent)
        if (validateContext) {
            String currentUserAgent = request.getHeader("User-Agent");
            String currentIp = getClientIp(request);

            if (!currentToken.matchesContext(currentUserAgent, currentIp)) {
                logger.warn("Context mismatch for token. User: {}, Original IP: {}, Current IP: {}",
                        currentToken.getUser().getEmail(),
                        currentToken.getIpAddress(),
                        currentIp);
                throw new RefreshTokenException("Token context mismatch. Please login again.");
            }
        }

        // ✅ Token Rotation: Criar novo token e revogar o atual
        if (rotationEnabled) {
            String newToken = jwtProvider.generateRefreshToken();
            String newTokenHash = hashToken(newToken);

            // Marcar token atual como substituído
            currentToken.setReplacedByToken(newTokenHash);
            currentToken.setRevoked(true);
            currentToken.setLastUsedAt(LocalDateTime.now());

            // Criar novo token
            RefreshToken newRefreshToken = RefreshToken.builder()
                    .user(currentToken.getUser())
                    .tokenHash(newTokenHash)
                    .userAgent(currentToken.getUserAgent())
                    .ipAddress(currentToken.getIpAddress())
                    .expiresAt(LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenExpiration() / 1000))
                    .build();

            repository.save(currentToken);
            repository.save(newRefreshToken);

            logger.info("Token rotated for user: {}", currentToken.getUser().getEmail());

            // Retornar novo token (não o hash)
            newRefreshToken.setTokenHash(newToken); // Usar setTokenHash temporariamente para transporte
            return newRefreshToken;
        }

        // Se rotação desabilitada, apenas atualizar last_used_at
        currentToken.setLastUsedAt(LocalDateTime.now());
        repository.save(currentToken);

        return currentToken;
    }

    /**
     * ✅ IMPLEMENTADO: Limite de dispositivos
     */
    private void enforceDeviceLimit(UUID userId) {
        long activeTokens = repository.countActiveTokensByUserId(userId, LocalDateTime.now());

        if (activeTokens >= maxDevices) {
            logger.info("Max devices reached for user. Removing oldest tokens.");
            var tokens = repository.findActiveTokensByUserId(userId);

            // Ordenar por data de criação e revogar os mais antigos
            tokens.stream()
                    .sorted((t1, t2) -> t1.getCreatedAt().compareTo(t2.getCreatedAt()))
                    .limit(activeTokens - maxDevices + 1)
                    .forEach(token -> {
                        token.setRevoked(true);
                        repository.save(token);
                    });
        }
    }

    @Transactional
    public void revokeToken(String token) {
        String tokenHash = hashToken(token);
        repository.findByTokenHash(tokenHash).ifPresent(rt -> {
            rt.setRevoked(true);
            repository.save(rt);
            logger.info("Token revoked for user: {}", rt.getUser().getEmail());
        });
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        repository.revokeAllByUserId(userId);
        logger.warn("All tokens revoked for user ID: {}", userId);
    }

    /**
     * Limpeza automática de tokens expirados e revogados
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        repository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
        logger.info("Expired and revoked tokens cleaned up");
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
