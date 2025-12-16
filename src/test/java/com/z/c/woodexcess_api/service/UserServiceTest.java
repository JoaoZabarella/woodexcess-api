package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.address.AddressRequest;
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
import com.z.c.woodexcess_api.model.enums.UserRole;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Complete Unit Tests")
@ActiveProfiles("test")
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserService userService;

    @NotNull
    private User user;
    private RegisterRequest registerRequest;
    private RegisterResponse registerResponse;
    private UserResponse userResponse;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "test@mail.com";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "$2a$10$encoded";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .name("Test User")
                .phone("123456789")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        List<AddressRequest> addresses = new ArrayList<>();

        registerRequest = new RegisterRequest(
                "New User",
                "new@mail.com",
                PASSWORD,
                "987654321",
                addresses);

        registerResponse = new RegisterResponse(
                UUID.randomUUID(),
                "New User",
                "new@mail.com",
                "987654321",
                true,
                UserRole.USER);


        userResponse = new UserResponse(
                USER_ID,
                "Test User",
                EMAIL,
                "123456789",
                "http://avatar.url/image.png",
                true,
                UserRole.USER,
                new ArrayList<>()
        );
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterUserSuccessfully() {
        when(repository.findByEmail(registerRequest.email())).thenReturn(Optional.empty());
        when(mapper.toEntity(registerRequest)).thenReturn(user);
        when(encoder.encode(anyString())).thenReturn(ENCODED_PASSWORD);
        when(repository.save(any(User.class))).thenReturn(user);
        when(mapper.toRegisterResponse(user)).thenReturn(registerResponse);

        RegisterResponse result = userService.registerUser(registerRequest);

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("new@mail.com");

        verify(repository).findByEmail(registerRequest.email());
        verify(encoder).encode(anyString());
        verify(repository).save(any(User.class));
        verify(mapper).toRegisterResponse(user);
    }

    @Test
    @DisplayName("Should throw EmailAlredyExistException when email exists")
    void shouldThrowExceptionWhenEmailExists() {
        when(repository.findByEmail(registerRequest.email())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.registerUser(registerRequest))
                .isInstanceOf(EmailAlreadyExistException.class)
                .hasMessage("Email already exists");

        verify(repository).findByEmail(registerRequest.email());
        verifyNoInteractions(encoder, mapper);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should return user by ID")
    void shouldReturnUserById() {
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(mapper.toUserResponse(user)).thenReturn(userResponse);

        Optional<UserResponse> result = userService.getUserByID(USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(USER_ID);
        assertThat(result.get().email()).isEqualTo(EMAIL);

        verify(repository).findById(USER_ID);
        verify(mapper).toUserResponse(user);
    }

    @Test
    @DisplayName("Should return empty when user not found by ID")
    void shouldReturnEmptyWhenUserNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        Optional<UserResponse> result = userService.getUserByID(nonExistentId);

        assertThat(result).isEmpty();

        verify(repository).findById(nonExistentId);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("Should update user successfully with same email")
    void shouldUpdateUserWithSameEmail() {
        UpdateUserRequest updateRequest = new UpdateUserRequest("Updated Name", EMAIL,"11971407689", "http://avatar.url/image.png");
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);
        when(mapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.updateUser(USER_ID, updateRequest);

        assertThat(result).isNotNull();
        assertThat(user.getName()).isEqualTo("Updated Name");

        verify(repository).findById(USER_ID);
        verify(repository).save(user);
        verify(mapper).toUserResponse(user);
        verify(repository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should update user with new email")
    void shouldUpdateUserWithNewEmail() {
        String newEmail = "newemail@mail.com";
        UpdateUserRequest updateRequest = new UpdateUserRequest("Updated Name", newEmail, "11971407689", "http://avatar.url/image.png");

        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(repository.findByEmail(newEmail)).thenReturn(Optional.empty());
        when(repository.save(user)).thenReturn(user);
        when(mapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.updateUser(USER_ID, updateRequest);

        assertThat(result).isNotNull();
        assertThat(user.getEmail()).isEqualTo(newEmail);

        verify(repository).findById(USER_ID);
        verify(repository).findByEmail(newEmail);
        verify(repository).save(user);
    }

    @Test
    @DisplayName("Should throw EmailAlredyExistException when new email exists")
    void shouldThrowExceptionWhenNewEmailExists() {
        String existingEmail = "existing@mail.com";
        UpdateUserRequest updateRequest = new UpdateUserRequest("Name", existingEmail, "11971407689", "http://avatar.url/image.png");
        User anotherUser = User.builder().id(UUID.randomUUID()).email(existingEmail).build();

        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(repository.findByEmail(existingEmail)).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> userService.updateUser(USER_ID, updateRequest))
                .isInstanceOf(EmailAlreadyExistException.class)
                .hasMessage("Email already exists");

        verify(repository).findById(USER_ID);
        verify(repository).findByEmail(existingEmail);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found for update")
    void shouldThrowExceptionWhenUserNotFoundForUpdate() {
        UUID nonExistentId = UUID.randomUUID();
        UpdateUserRequest updateRequest = new UpdateUserRequest("Name", "email@test.com", "11971407689", "http://avatar.url/image.png");

        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(nonExistentId, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");

        verify(repository).findById(nonExistentId);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should change password successfully")
    void shouldChangePasswordSuccessfully() {
        String currentPassword = "oldPassword";
        String newPassword = "newPassword123";
        String newEncodedPassword = "$2a$10$newEncoded";

        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);

        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(encoder.matches(currentPassword, ENCODED_PASSWORD)).thenReturn(true);
        when(encoder.encode(newPassword)).thenReturn(newEncodedPassword);

        userService.changePassword(USER_ID, request);

        assertThat(user.getPassword()).isEqualTo(newEncodedPassword);

        verify(repository).findById(USER_ID);
        verify(encoder).matches(currentPassword, ENCODED_PASSWORD);
        verify(encoder).encode(newPassword);
        verify(repository).save(user);
    }

    @Test
    @DisplayName("Should throw PasswordIncorrectException when current password is wrong")
    void shouldThrowExceptionWhenCurrentPasswordIsWrong() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", "newPassword");

        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(encoder.matches("wrongPassword", ENCODED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(USER_ID, request))
                .isInstanceOf(PasswordIncorrectException.class)
                .hasMessage("Invalid current password");

        verify(repository).findById(USER_ID);
        verify(encoder).matches("wrongPassword", ENCODED_PASSWORD);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found for password change")
    void shouldThrowExceptionWhenUserNotFoundForPasswordChange() {
        UUID nonExistentId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("current", "new");

        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(nonExistentId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");

        verify(repository).findById(nonExistentId);
        verifyNoInteractions(encoder);
    }

    @Test
    @DisplayName("Should return paginated list of users")
    void shouldReturnPaginatedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = List.of(user);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(repository.findAll(pageable)).thenReturn(userPage);
        when(mapper.toUserResponse(user)).thenReturn(userResponse);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(USER_ID);

        verify(repository).findAll(pageable);
        verify(mapper).toUserResponse(user);
    }

    @Test
    @DisplayName("Should return empty page when no users exist")
    void shouldReturnEmptyPageWhenNoUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(repository.findAll(pageable)).thenReturn(emptyPage);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(repository).findAll(pageable);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("Should deactivate user successfully")
    void shouldDeactivateUserSuccessfully() {
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        userService.deactivateUser(USER_ID);

        assertThat(user.getIsActive()).isFalse();

        verify(repository).findById(USER_ID);
        verify(repository).save(user);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found for deactivation")
    void shouldThrowExceptionWhenUserNotFoundForDeactivation() {
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");

        verify(repository).findById(nonExistentId);
        verify(repository, never()).save(any());
    }
}