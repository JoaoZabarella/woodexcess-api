package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.UserResponseDTO;
import com.z.c.woodexcess_api.exception.users.EmailAlredyExistException;
import com.z.c.woodexcess_api.mapper.UserMapper;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.role.UserRole;
import com.z.c.woodexcess_api.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service

public class UserService {

    @Autowired
    private UserRepository repository;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private JwtProvider jwt;

    public UserResponseDTO registerUser(RegisterRequest dto) throws IllegalAccessException {
        if(repository.findByEmail(dto.email()).isPresent()){
            throw new EmailAlredyExistException("Email already exists");
        }

        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(encoder.encode(dto.password()));
        user.setRole(UserRole.USER);

        User savedUser = repository.save(user);
        return UserMapper.toResponse(savedUser);

    }




}
