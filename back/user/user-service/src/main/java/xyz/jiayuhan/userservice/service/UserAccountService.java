package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userservice.model.request.account.ChangePasswordRequest;
import xyz.jiayuhan.userservice.model.request.account.UpdateProfileRequest;
import xyz.jiayuhan.userservice.model.request.account.UpdateStudentProfileRequest;
import xyz.jiayuhan.userservice.model.request.account.UpdateTeacherProfileRequest;
import xyz.jiayuhan.userservice.model.response.account.StudentProfileResponse;
import xyz.jiayuhan.userservice.model.response.account.TeacherProfileResponse;
import xyz.jiayuhan.userservice.model.response.account.UserProfileResponse;

/** 当前登录用户的资料与凭证维护服务。 */
public interface UserAccountService {

    /** @return 当前用户的安全资料、角色与权限，不包含密码哈希 */
    UserProfileResponse currentUser();

    /**
     * 修改当前用户的可编辑资料。
     *
     * @param request 资料修改参数
     * @return 修改后的用户资料
     */
    UserProfileResponse updateProfile(UpdateProfileRequest request);

    /**
     * 校验旧密码后修改密码，并使该账号全部已有会话失效。
     *
     * @param request 旧密码与新密码
     */
    void changePassword(ChangePasswordRequest request);

    StudentProfileResponse currentStudentProfile();

    StudentProfileResponse updateStudentProfile(UpdateStudentProfileRequest request);

    TeacherProfileResponse currentTeacherProfile();

    TeacherProfileResponse updateTeacherProfile(UpdateTeacherProfileRequest request);
}
