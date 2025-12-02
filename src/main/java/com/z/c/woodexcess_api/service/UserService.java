package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterResponse;
import com.z.c.woodexcess_api.dto.user.ChangePasswordRequest;
import com.z.c.woodexcess_api.dto.user.UpdateUserRequest;
import com.z.c.woodexcess_api.dto.user.UserResponse;
import com.z.c.woodexcess_api.exception.users.EmailAlreadyExistException;
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
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
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
        log.info("Registering new user with email: {}", dto.email());

        if (repository.findByEmail(dto.email()).isPresent()) {
            log.warn("Registration failed: email already exists: {}", dto.email());
            throw new EmailAlreadyExistException("Email already exists");
        }

        User user = mapper.toEntity(dto);
        user.setPassword(encoder.encode(dto.password()));
        User savedUser = repository.save(user);

        log.info("User registered successfully: {}", savedUser.getEmail());
        return mapper.toRegisterResponse(savedUser);
    }

    public Optional<UserResponse> getUserByID(UUID id) {
        log.debug("Fetching user by ID: {}", id);
        return repository.findById(id)
                .map(mapper::toUserResponse);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest dto) {
        log.info("Updating user: {}", id);

        var user = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("User update failed: user not found: {}", id);
                    return new EntityNotFoundException("User not found");
                });

        if (!user.getEmail().equals(dto.email())) {
            if (repository.findByEmail(dto.email()).isPresent()) {
                log.warn("User update failed: email already exists: {}", dto.email());
                throw new EmailAlreadyExistException("Email already exists");
            }
            user.setEmail(dto.email());
        }

        user.setName(dto.name());
        var updateUser = repository.save(user);

        log.info("User updated successfully: {}", id);
        return mapper.toUserResponse(updateUser);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest dto) {
        log.info("Changing password for user: {}", id);

        var user = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Password change failed: user not found: {}", id);
                    return new EntityNotFoundException("User not found");
                });

        if (!encoder.matches(dto.currentPassword(), user.getPassword())) {
            log.warn("Password change failed: invalid current password for user: {}", id);
            throw new PasswordIncorrectException("Invalid current password");
        }

        user.setPassword(encoder.encode(dto.newPassword()));
        repository.save(user);

        log.info("Password changed successfully for user: {}", id);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users, page: {}", pageable.getPageNumber());
        return repository.findAll(pageable).map(mapper::toUserResponse);
    }

    @Transactional
    public void deactivateUser(UUID id) {
        log.info("Deactivating user: {}", id);

        var user = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("User deactivation failed: user not found: {}", id);
                    return new EntityNotFoundException("User not found");
                });

        user.setActive(false);
        repository.save(user);

        log.info("User deactivated successfully: {}", id);
    }

    @Transactional
    public User findEntityById(UUID id) {
        log.debug("Finding user entity by ID: {}", id);
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found: {}", id);
                    return new EntityNotFoundException("User not found with id: " + id);
                });
    }
}
