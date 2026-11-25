package com.cms.mapper;

import com.cms.dto.request.RegisterRequest;
import com.cms.dto.response.UserResponse;
import com.cms.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    /** Builds a new user; email and phone must already be normalised and the password already hashed. */
    public User toNewUser(RegisterRequest request, String normalizedEmail, String normalizedPhone,
                          String passwordHash) {
        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setPasswordHash(passwordHash);
        return user;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getPhone(), user.getCreatedAt());
    }
}
