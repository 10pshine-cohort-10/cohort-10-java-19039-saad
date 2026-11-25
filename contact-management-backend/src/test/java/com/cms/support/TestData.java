package com.cms.support;

import com.cms.dto.request.RegisterRequest;
import com.cms.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestData {

    public static final String VALID_PASSWORD = "Str0ng!Pass1";

    private TestData() {
    }

    public static User user(long id, String email, String phone, String passwordHash) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordHash);
        return user;
    }

    public static RegisterRequest registerRequest(String email, String phone) {
        return new RegisterRequest(" Jane ", "Doe ", email, phone, VALID_PASSWORD, VALID_PASSWORD);
    }
}
