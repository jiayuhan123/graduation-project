package xyz.jiayuhan.userservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.userservice.entity.User;
import xyz.jiayuhan.userservice.mapper.UserMapper;
import xyz.jiayuhan.userservice.service.impl.UserDirectoryServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证供业务服务使用的用户权限判定。 */
@ExtendWith(MockitoExtension.class)
class UserDirectoryServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RbacService rbacService;
    @Mock
    private ProfileService profileService;

    @InjectMocks
    private UserDirectoryServiceImpl userDirectoryService;

    @Test
    void grantsPermissionOnlyToActiveUser() {
        User user = new User();
        user.setId(42L);
        user.setStatus(1);
        when(userMapper.selectById(42L)).thenReturn(user);
        when(rbacService.permissionCodes(42L)).thenReturn(List.of("content:manage"));

        var authorization = userDirectoryService.authorize(42L, "content:manage");

        assertThat(authorization.exists()).isTrue();
        assertThat(authorization.active()).isTrue();
        assertThat(authorization.granted()).isTrue();
    }

    @Test
    void disabledUserCannotReceivePermission() {
        User user = new User();
        user.setId(42L);
        user.setStatus(0);
        when(userMapper.selectById(42L)).thenReturn(user);

        var authorization = userDirectoryService.authorize(42L, "content:manage");

        assertThat(authorization.exists()).isTrue();
        assertThat(authorization.active()).isFalse();
        assertThat(authorization.granted()).isFalse();
        verify(rbacService, never()).permissionCodes(42L);
    }

    @Test
    void missingUserReturnsNegativeAuthorization() {
        when(userMapper.selectById(404L)).thenReturn(null);

        var authorization = userDirectoryService.authorize(404L, "content:manage");

        assertThat(authorization.exists()).isFalse();
        assertThat(authorization.active()).isFalse();
        assertThat(authorization.granted()).isFalse();
    }
}
