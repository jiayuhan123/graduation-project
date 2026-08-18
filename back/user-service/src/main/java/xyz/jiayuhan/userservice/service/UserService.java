package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userservice.model.dto.LoginDTO;
import xyz.jiayuhan.userservice.model.dto.RegisterDTO;
import xyz.jiayuhan.userservice.model.vo.LoginVO;
import xyz.jiayuhan.userservice.model.vo.RegisterVO;

/**
 * 用户账号服务
 */
public interface UserService {

    /**
     * 用户登录（用户名或手机号 + 密码）
     *
     * @param loginDTO 登录凭证
     * @return 登录结果（包含 Token）
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 用户注册
     *
     * @param registerDTO 注册信息
     * @return 新用户基础信息
     */
    RegisterVO register(RegisterDTO registerDTO);
}
