package com.cms.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.cms.dto.request.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegisterRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    private static RegisterRequest request(String email, String phone, String password, String confirm) {
        return new RegisterRequest("Jane", "Doe", email, phone, password, confirm);
    }

    private static Set<String> invalidFields(RegisterRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void validRequest_hasNoViolations() {
        assertThat(validator.validate(request("jane@example.com", "+1 555 123 4567", "Str0ng!Pass1", "Str0ng!Pass1")))
                .isEmpty();
    }

    @Test
    void mismatchedConfirmation_isReportedOnConfirmPasswordField() {
        Set<String> fields = invalidFields(request("jane@example.com", "+15551234567", "Str0ng!Pass1", "Different1!"));
        assertThat(fields).containsExactly("confirmPassword");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ab1!xyz", "alllowercase1!", "ALLUPPERCASE1!", "NoDigitsHere!", "NoSymbol1234", "Has Space Only 1"})
    void weakPasswords_areRejected(String password) {
        assertThat(invalidFields(request("jane@example.com", "+15551234567", password, password)))
                .contains("password");
    }

    @Test
    void passwordLongerThanBcryptLimit_isRejected() {
        String tooLong = "Aa1!" + "x".repeat(70);
        assertThat(invalidFields(request("jane@example.com", "+15551234567", tooLong, tooLong))).contains("password");
    }

    @ParameterizedTest
    @ValueSource(strings = {"plainaddress", "missing@tld", "two@@example.com", "with space@example.com"})
    void malformedEmails_areRejected(String email) {
        assertThat(invalidFields(request(email, "+15551234567", "Str0ng!Pass1", "Str0ng!Pass1"))).contains("email");
    }

    @Test
    void invalidPhone_isRejected() {
        assertThat(invalidFields(request("jane@example.com", "call me", "Str0ng!Pass1", "Str0ng!Pass1")))
                .containsExactly("phone");
    }

    @Test
    void blankRequiredFields_areRejected() {
        RegisterRequest blank = new RegisterRequest(" ", "", "", "", "", "");
        assertThat(invalidFields(blank)).contains("firstName", "lastName", "email", "phone", "password", "confirmPassword");
    }
}
