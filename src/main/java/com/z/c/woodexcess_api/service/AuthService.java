package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.exception.users.EmailAlredyExistException;
import com.z.c.woodexcess_api.exception.users.PasswordIncorrectException;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtProvider provider;

    public String authenticate(String email, String password) {
        var user = repository.findByEmail(email)
                .orElseThrow(() -> new PasswordIncorrectException("Invalid credentials"));
        if (!encoder.matches(password, user.getPassword())) {
            throw new PasswordIncorrectException("Invalid credentials");
        }
        return provider.generateJwtToken(user);
    }



}
