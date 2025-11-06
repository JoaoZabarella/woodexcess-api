package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.UserRegisterDTO;
import com.z.c.woodexcess_api.dto.UserResponseDTO;
import com.z.c.woodexcess_api.exception.users.EmailAlredyExist;
import com.z.c.woodexcess_api.mapper.UserMapper;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.role.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service

public class UserService {

    @Autowired
    private UserRepository repository;
    @Autowired
    private PasswordEncoder encoder;

    public UserResponseDTO registerUser(UserRegisterDTO dto) throws IllegalAccessException {
        if(repository.findByEmail(dto.email()).isPresent()){
            throw new EmailAlredyExist("Email already exists");
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
