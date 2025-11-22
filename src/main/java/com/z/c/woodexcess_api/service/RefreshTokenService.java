package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.TokenRotationResult;
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
import java.util.Arrays;
import java.util.Comparator;
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
    public TokenRotationResult validateAndRotate(String token, HttpServletRequest request) {
        // Gera hash SHA-256 do token para buscar no banco
        String tokenHash = hashToken(token);

        // Busca token no banco pelo hash
        RefreshToken currentToken = repository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RefreshTokenException("Invalid refresh token"));

        // Detecção de reuso: se token já foi substituído, possível ataque
        if (reuseDetectionEnabled && currentToken.getReplacedByToken() != null) {
            revokeAllUserTokens(currentToken.getUser().getId());
            throw new TokenReuseDetectedException("Token reuse detected. All sessions revoked.");
        }

        // Valida se o token não expirou
        if (currentToken.isExpired()) {
            throw new RefreshTokenException("Refresh token has expired");
        }

        // Valida se o token não foi revogado manualmente
        if (currentToken.getRevoked()) {
            throw new RefreshTokenException("Refresh token has been revoked");
        }

        // Valida contexto (IP + User-Agent) para detectar mudança de dispositivo
        if (validateContext) {
            String currentUserAgent = request.getHeader("User-Agent");
            String currentIp = getClientIp(request);
            if (!currentToken.matchesContext(currentUserAgent, currentIp)) {
                throw new RefreshTokenException("Token context mismatch. Please login again.");
            }
        }

        // Token Rotation: cria novo token e revoga o atual
        if (rotationEnabled) {
            // Gera novo token raw (UUID)
            String newToken = jwtProvider.generateRefreshToken();
            String newTokenHash = hashToken(newToken);
            // Marca token atual como substituído e revogado
            currentToken.setReplacedByToken(newTokenHash);
            currentToken.setRevoked(true);
            currentToken.setLastUsedAt(LocalDateTime.now());

            // Cria novo token no banco com o hash
            RefreshToken newRefreshToken = RefreshToken.builder()
                    .user(currentToken.getUser())
                    .tokenHash(newTokenHash)
                    .userAgent(currentToken.getUserAgent())
                    .ipAddress(currentToken.getIpAddress())
                    .expiresAt(LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenExpiration() / 1000))
                    .build();

            // Salva ambos os tokens no banco
            repository.save(currentToken);
            repository.save(newRefreshToken);

            // Retorna DTO com token raw (para o cliente) e hash separados
            return TokenRotationResult.builder()
                    .rawToken(newToken)        // Token UUID original (enviar ao cliente)
                    .tokenHash(newTokenHash)   // Hash SHA-256 (já no banco)
                    .userId(currentToken.getUser().getId())
                    .build();
        }

        // Se rotação desabilitada, apenas atualiza last_used_at
        currentToken.setLastUsedAt(LocalDateTime.now());
        repository.save(currentToken);

        // Retorna token original sem rotacionar
        return TokenRotationResult.builder()
                .rawToken(token) // Token original enviado pelo cliente
                .tokenHash(tokenHash)
                .userId(currentToken.getUser().getId())
                .build();
    }

    /**
     * ✅ Limite de dispositivos
     */
    private void enforceDeviceLimit(UUID userId) {
        long activeTokens = repository.countActiveTokensByUserId(userId, LocalDateTime.now());

        if (activeTokens >= maxDevices) {
            logger.info("Max devices reached for user. Removing oldest tokens.");
            var tokens = repository.findActiveTokensByUserId(userId);

            // Ordenar por data de criação e revogar os mais antigos
            tokens.stream()
                    .sorted(Comparator.comparing(RefreshToken::getCreatedAt))
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

        // Handle proxy chains
        if (ip != null && ip.contains(",")) {
            ip = Arrays.toString(ip.split(",")).trim();
        }

        return ip != null ? ip : "unknown";  // Nunca retornar null
    }
}
