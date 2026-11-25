package com.cms.service;

import com.cms.dto.request.ChangePasswordRequest;
import com.cms.dto.request.UpdateProfileRequest;
import com.cms.dto.response.UserResponse;

public interface UserService {

    UserResponse getProfile(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
