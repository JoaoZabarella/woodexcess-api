package com.z.c.woodexcess_api.service.security;

import com.z.c.woodexcess_api.dto.auth.LoginResponse;
import com.z.c.woodexcess_api.dto.auth.TokenRotationResult;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtProvider provider;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.access-expiration-ms}")
    private long accessTokenExpiration;

    @Autowired
    public AuthService(UserRepository repository, PasswordEncoder encoder, JwtProvider provider,
            RefreshTokenService refreshTokenService) {
        this.repository = repository;
        this.encoder = encoder;
        this.provider = provider;
        this.refreshTokenService = refreshTokenService;
    }

    public LoginResponse authenticate(String email, String password, HttpServletRequest request) {
        log.info("Authentication attempt for user: {}", email);

        var user = repository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: user not found for email: {}", email);
                    return new BadCredentialsException("Invalid credentials");
                });

        if (!encoder.matches(password, user.getPassword())) {
            log.warn("Authentication failed: invalid password for user: {}", email);
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.getActive()) {
            log.warn("Authentication failed: inactive account for user: {}", email);
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = provider.generateJwtToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user, request);

        log.info("User authenticated successfully: {}", email);
        return new LoginResponse(accessToken, refreshToken, accessTokenExpiration);
    }

    public LoginResponse refreshAccessToken(String refreshToken, HttpServletRequest request) {
        log.debug("Refreshing access token");

        TokenRotationResult result = refreshTokenService.validateAndRotate(refreshToken, request);

        var user = repository.findById(result.userId())
                .orElseThrow(() -> {
                    log.error("Token refresh failed: user not found for ID: {}", result.userId());
                    return new BadCredentialsException("User not found");
                });

        String newAccessToken = provider.generateJwtToken(user);
        String newRefreshToken = result.rawToken();

        log.info("Access token refreshed successfully for user: {}", user.getEmail());
        return new LoginResponse(newAccessToken, newRefreshToken, accessTokenExpiration);
    }

    @Transactional
    public void logout(String refreshToken) {
        log.info("User logout initiated");
        refreshTokenService.revokeToken(refreshToken);
        log.debug("Refresh token revoked successfully");
    }
}
