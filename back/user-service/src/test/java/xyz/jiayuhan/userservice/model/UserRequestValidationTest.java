package xyz.jiayuhan.userservice.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import xyz.jiayuhan.userservice.model.dto.RegisterDTO;

import static org.assertj.core.api.Assertions.assertThat;

class UserRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsExistingClientRegistrationShape() {
        RegisterDTO request = new RegisterDTO(
                "demo_user", "123456", null, "测试用户", null, "13800138000");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidUsernameAndPhone() {
        RegisterDTO request = new RegisterDTO(
                "1bad", "123456", null, null, null, "123");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("username", "phone");
    }
}
