package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.LoginResponse;
import com.z.c.woodexcess_api.exception.users.PasswordIncorrectException;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtProvider provider;

    @Autowired
    public AuthService(UserRepository repository, PasswordEncoder encoder, JwtProvider provider) {
        this.repository = repository;
        this.encoder = encoder;
        this.provider = provider;
    }

    public LoginResponse authenticate(String email, String password) {
        var user = repository.findByEmail(email)
                .orElseThrow(() -> new PasswordIncorrectException("Invalid credentials"));
        if (!encoder.matches(password, user.getPassword())) {
            throw new PasswordIncorrectException("Invalid credentials");
        }
        String token = provider.generateJwtToken(user);
        return new LoginResponse(token, user.getName(), user.getEmail(),  user.getRole());
    }
}

