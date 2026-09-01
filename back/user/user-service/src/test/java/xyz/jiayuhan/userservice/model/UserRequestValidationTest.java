package xyz.jiayuhan.userservice.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import xyz.jiayuhan.userservice.model.request.auth.RegisterRequest;

import static org.assertj.core.api.Assertions.assertThat;

class UserRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsExistingClientRegistrationShape() {
        RegisterRequest request = new RegisterRequest(
                "demo_user", "123456", null, "测试用户", null, "13800138000", 3, 1);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidUsernameAndPhone() {
        RegisterRequest request = new RegisterRequest(
                "1bad", "123456", null, null, null, "123", 7, 3);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("username", "phone", "gradeLevel", "semester");
    }
}
