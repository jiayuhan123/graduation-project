package xyz.jiayuhan.userservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.userservice.entity.User;
import xyz.jiayuhan.userservice.mapper.UserMapper;
import xyz.jiayuhan.userservice.model.dto.LoginDTO;
import xyz.jiayuhan.userservice.model.dto.RegisterDTO;
import xyz.jiayuhan.userservice.model.vo.RegisterVO;
import xyz.jiayuhan.userservice.service.impl.AuthServiceImpl;
import xyz.jiayuhan.userservice.service.impl.UserDataSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserDataSupport userDataSupport;
    @Mock
    private RbacService rbacService;
    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerHashesPasswordAndAssignsDefaultRole() {
        RegisterDTO request = new RegisterDTO(
                "Demo_User", "secret12", "演示用户", null, "USER@example.com", "13800138000");
        when(userDataSupport.normalizeUsername("Demo_User")).thenReturn("demo_user");
        when(userDataSupport.normalizeEmail("USER@example.com")).thenReturn("user@example.com");
        when(userDataSupport.trimToNull("13800138000")).thenReturn("13800138000");
        when(userDataSupport.trimToNull("演示用户")).thenReturn("演示用户");
        when(passwordEncoder.encode("secret12")).thenReturn("bcrypt-hash");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        RegisterVO result = authService.register(request);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.username()).isEqualTo("demo_user");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
        verify(rbacService).assignDefaultRole(100L);
    }

    @Test
    void loginUsesSameErrorForUnknownAccount() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login(
                new LoginDTO("unknown", "wrong-password"), "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.USERNAME_OR_PASSWORD_ERROR.getCode()));

        verify(loginAttemptService).recordFailure("unknown");
    }

    @Test
    void disabledAccountCannotLogin() {
        User user = new User();
        user.setId(1L);
        user.setPasswordHash("hash");
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                new LoginDTO("demo", "password"), "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.USER_DISABLED.getCode()));
    }
}
