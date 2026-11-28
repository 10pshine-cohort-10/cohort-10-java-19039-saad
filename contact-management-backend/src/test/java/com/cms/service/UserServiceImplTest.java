package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cms.dto.request.ChangePasswordRequest;
import com.cms.dto.request.UpdateProfileRequest;
import com.cms.dto.response.UserResponse;
import com.cms.entity.User;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.InvalidCredentialsException;
import com.cms.exception.ValidationException;
import com.cms.mapper.UserMapper;
import com.cms.repository.UserRepository;
import com.cms.service.impl.UserServiceImpl;
import com.cms.support.TestData;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    private UserMapper userMapper;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        service = new UserServiceImpl(userRepository, passwordEncoder, refreshTokenService, userMapper);
    }

    @Test
    void updateProfile_updatesUserDetails() {
        Long userId = 1L;
        User user = TestData.user(userId, "test@example.com", "+15551234567", "hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith", "+15559998888");
        UserResponse response = service.updateProfile(userId, request);

        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.lastName()).isEqualTo("Smith");
        assertThat(response.phone()).isEqualTo("+15559998888");
    }

    @Test
    void updateProfile_whenPhoneConflict_throwsDuplicateResource() {
        Long userId = 1L;
        User user = TestData.user(userId, "test@example.com", "+15551234567", "hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhone("+15559998888")).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith", "+15559998888");

        assertThatThrownBy(() -> service.updateProfile(userId, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void changePassword_success_updatesHashAndIncrementsTokenVersion() {
        Long userId = 1L;
        User user = TestData.user(userId, "test@example.com", "+15551234567", "oldHash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("NewStr0ng!Pass")).thenReturn("newHash");

        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "NewStr0ng!Pass", "NewStr0ng!Pass");

        service.changePassword(userId, request);

        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        assertThat(user.getTokenVersion()).isEqualTo(1);
        verify(refreshTokenService).revokeAllForUser(userId);
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_withWrongCurrentPassword_throwsInvalidCredentials() {
        Long userId = 1L;
        User user = TestData.user(userId, "test@example.com", "+15551234567", "oldHash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "oldHash")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "NewStr0ng!Pass", "NewStr0ng!Pass");

        assertThatThrownBy(() -> service.changePassword(userId, request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void changePassword_withMismatchedConfirmPassword_throwsValidationException() {
        Long userId = 1L;
        User user = TestData.user(userId, "test@example.com", "+15551234567", "oldHash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "NewStr0ng!Pass", "DifferentPass!");

        assertThatThrownBy(() -> service.changePassword(userId, request))
                .isInstanceOf(ValidationException.class);
    }
}
