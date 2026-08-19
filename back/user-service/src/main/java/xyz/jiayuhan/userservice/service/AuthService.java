package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userservice.model.dto.LoginDTO;
import xyz.jiayuhan.userservice.model.dto.RegisterDTO;
import xyz.jiayuhan.userservice.model.vo.LoginVO;
import xyz.jiayuhan.userservice.model.vo.RegisterVO;
import xyz.jiayuhan.userservice.model.vo.SessionVO;

/** 注册、登录与会话查询服务。 */
public interface AuthService {

    /**
     * 使用用户名、手机号或邮箱登录。
     *
     * @param request  登录凭证
     * @param clientIp 客户端 IP，用于登录审计，可为空
     * @return 新会话令牌及授权快照
     */
    LoginVO login(LoginDTO request, String clientIp);

    /**
     * 创建普通用户账号并授予默认角色。
     *
     * @param request 注册资料
     * @return 新用户标识
     */
    RegisterVO register(RegisterDTO request);

    /** 注销当前令牌对应的会话。 */
    void logout();

    /**
     * 获取当前会话及实时角色、权限信息。
     *
     * @return 当前会话信息
     */
    SessionVO currentSession();
}
