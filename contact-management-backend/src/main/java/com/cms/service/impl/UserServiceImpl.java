package com.cms.service.impl;

import com.cms.dto.request.ChangePasswordRequest;
import com.cms.dto.request.UpdateProfileRequest;
import com.cms.dto.response.FieldErrorDetail;
import com.cms.dto.response.UserResponse;
import com.cms.entity.User;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.InvalidCredentialsException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.ValidationException;
import com.cms.mapper.UserMapper;
import com.cms.repository.UserRepository;
import com.cms.service.RefreshTokenService;
import com.cms.service.UserService;
import com.cms.util.PhoneNormalizer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           RefreshTokenService refreshTokenService,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String normalizedPhone = PhoneNormalizer.normalize(request.phone());

        // Check if phone number is changing and already taken by another user
        if (!normalizedPhone.equals(user.getPhone()) && userRepository.existsByPhone(normalizedPhone)) {
            throw new DuplicateResourceException("Phone number is already in use by another account.",
                    List.of(new FieldErrorDetail("phone", "This phone number is already registered")));
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(normalizedPhone);

        User saved = userRepository.save(user);
        log.info("User profile updated: userId={}", userId);

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // 1. Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            log.warn("Password change failed: incorrect current password for userId={}", userId);
            throw new InvalidCredentialsException("The current password you provided is incorrect.");
        }

        // 2. Verify confirmation matches
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new ValidationException("NEW_PASSWORD_MISMATCH",
                    List.of(new FieldErrorDetail("confirmNewPassword", "New password and confirmation do not match.")));
        }

        // 3. Update password hash
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        // 4. Invalidate all existing access tokens and revoke refresh tokens
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);

        log.info("Password changed successfully and sessions revoked: userId={}", userId);
    }
}
