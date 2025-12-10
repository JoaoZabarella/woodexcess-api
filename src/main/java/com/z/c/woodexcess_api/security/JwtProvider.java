package com.z.c.woodexcess_api.security;

import com.z.c.woodexcess_api.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    private static final int MINIMUM_SECRET_BITS = 512;
    private static final int MINIMUM_SECRET_BYTES = MINIMUM_SECRET_BITS / 8;
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "type";

    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-expiration-ms}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshTokenExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        validateSecret();
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT Provider initialized successfully with HS512 algorithm");
    }

    private void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret cannot be null or empty");
        }

        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    String.format(
                            "JWT secret must be at least %d bits (%d bytes) for %s. Current: %d bytes",
                            MINIMUM_SECRET_BITS,
                            MINIMUM_SECRET_BYTES,
                            SIGNATURE_ALGORITHM.getValue(),
                            jwtSecret.getBytes(StandardCharsets.UTF_8).length
                    )
            );
        }

        log.debug("JWT secret validation passed: {} bytes",
                jwtSecret.getBytes(StandardCharsets.UTF_8).length);
    }

    public String generateJwtToken(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (user.getRole() == null) {
            throw new IllegalArgumentException("User role cannot be null");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId().toString());
        claims.put(CLAIM_ROLE, user.getRole().toString());
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);

        String token = buildToken(user.getEmail(), claims, accessTokenExpiration);

        log.debug("Generated access token for user: {} (userId: {})",
                user.getEmail(), user.getId());

        return token;
    }

    public String generateToken(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);

        String token = buildToken(email, claims, accessTokenExpiration);

        log.debug("Generated minimal access token for email: {}", email);

        return token;
    }

    private String buildToken(String subject, Map<String, Object> claims, long expirationMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey, SIGNATURE_ALGORITHM)
                .compact();
    }

    public String generateRefreshToken() {
        String token = String.format("%s-%s", UUID.randomUUID(), UUID.randomUUID());
        log.debug("Generated refresh token");
        return token;
    }

    public boolean validateJwtToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("Validation failed: token is null or empty");
            return false;
        }

        try {
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            log.debug("Token validation successful");
            return true;

        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token format: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error validating JWT: {}", e.getMessage(), e);
        }

        return false;
    }

    public String getEmailFromToken(String token) {
        Claims claims = extractClaims(token);
        String email = claims.getSubject();

        log.debug("Extracted email from token: {}", email);
        return email;
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        String userIdStr = claims.get(CLAIM_USER_ID, String.class);

        if (userIdStr == null) {
            throw new JwtException("Token does not contain userId claim");
        }

        try {
            UUID userId = UUID.fromString(userIdStr);
            log.debug("Extracted userId from token: {}", userId);
            return userId;
        } catch (IllegalArgumentException e) {
            throw new JwtException("Invalid userId format in token: " + userIdStr, e);
        }
    }

    public String getRoleFromToken(String token) {
        Claims claims = extractClaims(token);
        String role = claims.get(CLAIM_ROLE, String.class);

        if (role == null) {
            throw new JwtException("Token does not contain role claim");
        }

        log.debug("Extracted role from token: {}", role);
        return role;
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractClaims(token);
            Date expiration = claims.getExpiration();
            boolean expired = expiration.before(new Date());

            log.debug("Token expiration check: expired={}, expiresAt={}",
                    expired, expiration);

            return expired;
        } catch (ExpiredJwtException e) {
            log.debug("Token is expired: {}", e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = extractClaims(token);
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();

            return Math.max(0, remaining);
        } catch (Exception e) {
            log.error("Error calculating remaining time: {}", e.getMessage());
            return 0;
        }
    }

    private Claims extractClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Failed to extract claims from token: {}", e.getMessage());
            throw e;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public String getSignatureAlgorithm() {
        return SIGNATURE_ALGORITHM.getValue();
    }
}
