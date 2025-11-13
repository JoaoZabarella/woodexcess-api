package com.z.c.woodexcess_api.service;

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

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtProvider provider;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.access-expiration-ms}")
    private long accessTokenExpiration;
    @Autowired
    public AuthService(UserRepository repository, PasswordEncoder encoder, JwtProvider provider, RefreshTokenService refreshTokenService) {
        this.repository = repository;
        this.encoder = encoder;
        this.provider = provider;
        this.refreshTokenService = refreshTokenService;
    }

    public LoginResponse authenticate(String email, String password, HttpServletRequest request) {
        var user = repository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!encoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if(!user.getActive()){
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = provider.generateJwtToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user, request);
        return new LoginResponse(accessToken, refreshToken, accessTokenExpiration);
    }

    public LoginResponse refreshAccessToken(String refreshToken, HttpServletRequest request) {
        // Recebe DTO com token RAW separado do hash
        TokenRotationResult result = refreshTokenService.validateAndRotate(refreshToken, request);

        // Buscar usuário pelo ID (mais eficiente)
        var user = repository.findById(result.getUserId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Gerar novo access token
        String newAccessToken = provider.generateJwtToken(user);

        // Retornar token RAW (não o hash)
        String newRefreshToken = result.getRawToken();

        return new LoginResponse(newAccessToken, newRefreshToken, accessTokenExpiration);
    }

    @Transactional
    public void logout (String refreshToken){
        refreshTokenService.revokeToken(refreshToken);
    }
}

