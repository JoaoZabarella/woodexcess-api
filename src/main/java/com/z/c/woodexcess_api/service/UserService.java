package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterResponse;
import com.z.c.woodexcess_api.dto.user.ChangePasswordRequest;
import com.z.c.woodexcess_api.dto.user.UpdateUserRequest;
import com.z.c.woodexcess_api.dto.user.UserResponse;
import com.z.c.woodexcess_api.exception.users.EmailAlredyExistException;
import com.z.c.woodexcess_api.exception.users.PasswordIncorrectException;
import com.z.c.woodexcess_api.mapper.UserMapper;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

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

    @Transactional
    public RegisterResponse registerUser(RegisterRequest dto) {
        if (repository.findByEmail(dto.getEmail()).isPresent()) {
            throw new EmailAlredyExistException("Email already exists");
        }
        User user = mapper.toEntity(dto);
        user.setPassword(encoder.encode(dto.getPassword()));
        User savedUser = repository.save(user);
        return mapper.toRegisterResponse(savedUser);
    }

    public Optional<UserResponse> getUserByID(UUID id){
        return repository.findById(id)
                .map(mapper::toUserResponse);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest dto){
        var user = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if(!user.getEmail().equals(dto.email())){
            if(repository.findByEmail(dto.email()).isPresent()){
                throw new EmailAlredyExistException("Email already exists");
            }
            user.setEmail(dto.email());
        }
        user.setName(dto.name());
        var updateUser = repository.save(user);
        return mapper.toUserResponse(updateUser);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest dto){
        var user = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if(!encoder.matches(dto.currentPassword(), user.getPassword())){
            throw new PasswordIncorrectException("Invalid current password");
        }
        user.setPassword(encoder.encode(dto.newPassword()));
        repository.save(user);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable){
        return repository.findAll(pageable).map(mapper::toUserResponse);
    }
    @Transactional
    public void deactivateUser(UUID id) {
        var user = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setActive(false);
        repository.save(user);
    }

    @Transactional
    public User findEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }
}
