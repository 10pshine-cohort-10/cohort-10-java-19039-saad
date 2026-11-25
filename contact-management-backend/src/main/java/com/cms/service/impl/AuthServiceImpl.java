package com.cms.service.impl;

import com.cms.dto.request.LoginRequest;
import com.cms.dto.request.RegisterRequest;
import com.cms.dto.response.AuthResponse;
import com.cms.dto.response.FieldErrorDetail;
import com.cms.dto.response.UserResponse;
import com.cms.entity.User;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.InvalidCredentialsException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.UnauthorizedException;
import com.cms.mapper.UserMapper;
import com.cms.repository.UserRepository;
import com.cms.security.JwtService;
import com.cms.service.AuthService;
import com.cms.service.RefreshTokenService;
import com.cms.util.EmailNormalizer;
import com.cms.util.LogMasker;
import com.cms.util.PhoneNormalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String DUPLICATE_MESSAGE = "An account with these details already exists.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    /** Hash checked when the user does not exist so response time does not reveal which accounts exist. */
    private final String timingEqualizationHash;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                           RefreshTokenService refreshTokenService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.timingEqualizationHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        String phone = PhoneNormalizer.normalize(request.phone());

        List<FieldErrorDetail> conflicts = new ArrayList<>();
        if (userRepository.existsByEmail(email)) {
            conflicts.add(new FieldErrorDetail("email", "This email address is already registered"));
        }
        if (userRepository.existsByPhone(phone)) {
            conflicts.add(new FieldErrorDetail("phone", "This phone number is already registered"));
        }
        if (!conflicts.isEmpty()) {
            log.warn("Registration rejected, duplicate details: fields={}",
                    conflicts.stream().map(FieldErrorDetail::field).toList());
            throw new DuplicateResourceException(DUPLICATE_MESSAGE, conflicts);
        }

        User user = userMapper.toNewUser(request, email, phone, passwordEncoder.encode(request.password()));
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent registrations passed the checks above; the unique constraints decide the winner.
            log.warn("Registration rejected by unique constraint", e);
            throw new DuplicateResourceException(DUPLICATE_MESSAGE, List.of());
        }
        log.info("User registered: userId={}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        Optional<User> found = identifier.contains("@")
                ? userRepository.findByEmail(EmailNormalizer.normalize(identifier))
                : userRepository.findByPhone(PhoneNormalizer.normalize(identifier));

        String hashToCheck = found.map(User::getPasswordHash).orElse(timingEqualizationHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (found.isEmpty() || !passwordMatches) {
            log.warn("Login failed: identifier={}", LogMasker.maskIdentifier(identifier));
            throw new InvalidCredentialsException();
        }
        User user = found.get();
        log.info("Login succeeded: userId={}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refresh(String refreshToken) {
        Long userId = refreshTokenService.consume(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN",
                        "Your session is no longer valid. Please sign in again."));
        log.debug("Session refreshed: userId={}", userId);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        Optional<Long> userId = refreshTokenService.revoke(refreshToken);
        userId.ifPresentOrElse(
                id -> log.info("User logged out: userId={}", id),
                () -> log.debug("Logout requested without a live refresh token"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user.getId());
        return AuthResponse.bearer(accessToken, refreshToken, jwtService.getAccessTokenTtlSeconds(),
                userMapper.toResponse(user));
    }
}
