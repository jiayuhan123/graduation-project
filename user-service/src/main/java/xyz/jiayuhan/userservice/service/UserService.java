package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userservice.model.dto.LoginDTO;
import xyz.jiayuhan.userservice.model.vo.LoginVO;

public interface UserService {
    /**
     * 用户登录接口
     * @param loginDTO 登录凭证
     * @return 登录结果(包含 Token)
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 用户注册接口
     * @param registerDTO 注册信息
     */
    void register(xyz.jiayuhan.userservice.model.dto.RegisterDTO registerDTO);
}
