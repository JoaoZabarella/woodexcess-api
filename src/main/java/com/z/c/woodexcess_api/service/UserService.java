package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterResponse;
import com.z.c.woodexcess_api.exception.users.EmailAlredyExistException;
import com.z.c.woodexcess_api.mapper.UserMapper;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final UserMapper mapper;

    public UserService(UserRepository repository, PasswordEncoder encoder, UserMapper mapper) {
        this.repository = repository;
        this.encoder = encoder;
        this.mapper = mapper;
    }

    public RegisterResponse registerUser(RegisterRequest dto) {
        if (repository.findByEmail(dto.email()).isPresent()) {
            throw new EmailAlredyExistException("Email already exists");
        }
        User user = mapper.toEntity(dto);
        user.setPassword(encoder.encode(dto.password()));
        User savedUser = repository.save(user);
        return mapper.toResponse(savedUser);
    }
}
