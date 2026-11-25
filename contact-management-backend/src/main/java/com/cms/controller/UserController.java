package com.cms.controller;

import com.cms.dto.request.ChangePasswordRequest;
import com.cms.dto.request.UpdateProfileRequest;
import com.cms.dto.response.UserResponse;
import com.cms.security.UserPrincipal;
import com.cms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get user profile for authenticated user")
    public UserResponse getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getProfile(principal.id());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile")
    public UserResponse updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.id(), request);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password and revoke all active sessions")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.id(), request);
        return ResponseEntity.noContent().build();
    }
}
