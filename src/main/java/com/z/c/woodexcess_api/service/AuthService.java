package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.LoginResponse;
import com.z.c.woodexcess_api.exception.users.PasswordIncorrectException;
import com.z.c.woodexcess_api.model.RefreshToken;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
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
        RefreshToken rotatedToken = refreshTokenService.validateAndRotate(refreshToken, request);


        var user = rotatedToken.getUser();
        String newAccessToken = provider.generateJwtToken(user);

        //Return new refresh token if you have rotation
        String newRefreshToken = rotatedToken.getTokenHash();

        return new LoginResponse(newAccessToken, newRefreshToken, accessTokenExpiration);
    }

    public void logout (String refreshToken){
        refreshTokenService.revokeToken(refreshToken);
    }
}

