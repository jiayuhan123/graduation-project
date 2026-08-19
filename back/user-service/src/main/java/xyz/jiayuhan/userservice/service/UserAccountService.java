package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userservice.model.dto.ChangePasswordDTO;
import xyz.jiayuhan.userservice.model.dto.UpdateProfileDTO;
import xyz.jiayuhan.userservice.model.vo.UserProfileVO;

/** 当前登录用户的资料与凭证维护服务。 */
public interface UserAccountService {

    /** @return 当前用户的安全资料、角色与权限，不包含密码哈希 */
    UserProfileVO currentUser();

    /**
     * 修改当前用户的可编辑资料。
     *
     * @param request 资料修改参数
     * @return 修改后的用户资料
     */
    UserProfileVO updateProfile(UpdateProfileDTO request);

    /**
     * 校验旧密码后修改密码，并使该账号全部已有会话失效。
     *
     * @param request 旧密码与新密码
     */
    void changePassword(ChangePasswordDTO request);
}
