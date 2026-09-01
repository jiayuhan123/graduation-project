package xyz.jiayuhan.userservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.userservice.entity.Role;
import xyz.jiayuhan.userservice.entity.UserRole;
import xyz.jiayuhan.userservice.mapper.PermissionMapper;
import xyz.jiayuhan.userservice.mapper.RoleMapper;
import xyz.jiayuhan.userservice.mapper.RolePermissionMapper;
import xyz.jiayuhan.userservice.mapper.UserRoleMapper;
import xyz.jiayuhan.userservice.service.impl.RbacServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacServiceImplTest {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @InjectMocks
    private RbacServiceImpl rbacService;

    @Test
    void assignsConfiguredDefaultRole() {
        Role role = new Role();
        role.setId(7L);
        role.setCode("student");
        role.setStatus(1);
        when(roleMapper.selectOne(any())).thenReturn(role);

        rbacService.assignDefaultRole(100L);

        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(100L);
        assertThat(captor.getValue().getRoleId()).isEqualTo(7L);
    }

    @Test
    void rejectsUnknownOrDisabledRoleDuringReplacement() {
        assertThatThrownBy(() -> rbacService.replaceUserRoles(100L, List.of(1L, 2L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.ROLE_NOT_FOUND.getCode()));
    }
}
