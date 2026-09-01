package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userservice.model.request.admin.AdminCreateUserRequest;
import xyz.jiayuhan.userservice.model.request.admin.AdminUpdateUserRequest;
import xyz.jiayuhan.userservice.model.request.admin.AdminStudentProfileRequest;
import xyz.jiayuhan.userservice.model.request.admin.AdminTeacherProfileRequest;
import xyz.jiayuhan.userservice.model.response.admin.AdminUserResponse;
import xyz.jiayuhan.common.core.model.PageResult;

import java.util.List;

/**
 * 管理端用户维护服务。
 *
 * <p>所有修改操作均由控制器层完成权限校验；涉及账号安全状态的操作会在实现层
 * 主动使目标用户会话失效，避免旧会话继续持有变更前的权限。</p>
 */
public interface AdminUserService {

    /**
     * 按关键字和状态分页查询未删除用户。
     *
     * @param page    页码，从 1 开始
     * @param size    每页数量，最大为 100
     * @param keyword 用户名、昵称、姓名、邮箱或手机号关键字，可为空
     * @param status  账号状态：1 正常、0 禁用，可为空
     * @param roleCode 角色编码，可为空
     * @return 管理端用户分页数据
     */
    PageResult<AdminUserResponse> page(long page, long size, String keyword, Integer status, String roleCode);

    /**
     * 查询指定用户详情及其当前有效角色。
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    AdminUserResponse detail(Long userId);

    /**
     * 创建用户，并按请求设置初始状态和角色。
     *
     * @param request 创建参数
     * @return 创建后的用户详情
     */
    AdminUserResponse create(AdminCreateUserRequest request);

    /**
     * 修改用户的非安全资料；密码、状态和角色须通过各自的专用方法修改。
     *
     * @param userId  用户 ID
     * @param request 资料修改参数
     * @return 修改后的用户详情
     */
    AdminUserResponse update(Long userId, AdminUpdateUserRequest request);

    /**
     * 启用或禁用用户；禁用后立即注销该账号的全部会话。
     *
     * @param userId 用户 ID
     * @param status 1 正常、0 禁用
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 以请求中的角色集合完整替换用户现有角色，并注销其全部会话。
     *
     * @param userId  用户 ID
     * @param roleIds 新的角色 ID 集合
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 管理员重置其他用户的密码，并注销该账号的全部会话。
     *
     * @param userId      用户 ID
     * @param newPassword 新明文密码，仅在方法调用期间存在
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 逻辑删除用户、清理其角色关系并注销全部会话。
     *
     * @param userId 用户 ID
     */
    void delete(Long userId);

    /** 创建或更新指定用户的学生资料。 */
    AdminUserResponse upsertStudentProfile(Long userId, AdminStudentProfileRequest request);

    /** 创建或更新指定用户的教师资料。 */
    AdminUserResponse upsertTeacherProfile(Long userId, AdminTeacherProfileRequest request);
}
