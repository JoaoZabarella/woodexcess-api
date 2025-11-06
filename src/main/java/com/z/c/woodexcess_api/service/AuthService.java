package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterResponse;
import com.z.c.woodexcess_api.exception.users.EmailAlredyExistException;
import com.z.c.woodexcess_api.exception.users.PasswordIncorrectException;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.role.UserRole;
import com.z.c.woodexcess_api.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {
    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtProvider provider;

    public String authenticate(String email, String password) {
        var user = repository.findByEmail(email).
                orElseThrow(() -> new EmailAlredyExistException("User not found"));
        if(!encoder.matches(password, user.getPassword())){
            throw new PasswordIncorrectException("Invalid password");
        }
        return provider.generateJwtToken(user);
    }

    public RegisterResponse register(RegisterRequest request) {
        if(repository.findByEmail(request.email()).isPresent()){
            throw new EmailAlredyExistException("User with this email already exists");
        }

        var user = repository.save( new User(
                null,
                request.name(),
                request.email(),
                encoder.encode(request.password()),
                UserRole.USER
            )
        );

        return new RegisterResponse(user.getId().toString(), user.getEmail(),  user.getName());
    }
}
