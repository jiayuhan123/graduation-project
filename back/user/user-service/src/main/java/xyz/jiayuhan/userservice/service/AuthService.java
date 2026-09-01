package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userservice.model.request.auth.LoginRequest;
import xyz.jiayuhan.userservice.model.request.auth.RegisterRequest;
import xyz.jiayuhan.userservice.model.request.auth.WechatLoginRequest;
import xyz.jiayuhan.userservice.model.response.auth.LoginResponse;
import xyz.jiayuhan.userservice.model.response.auth.RegisterResponse;
import xyz.jiayuhan.userservice.model.response.auth.SessionResponse;

/** 注册、登录与会话查询服务。 */
public interface AuthService {

    /**
     * 使用用户名、手机号或邮箱登录。
     *
     * @param request  登录凭证
     * @param clientIp 客户端 IP，用于登录审计，可为空
     * @return 新会话令牌及授权快照
     */
    LoginResponse login(LoginRequest request, String clientIp);

    /** 使用微信小程序临时 code 登录；首次登录时自动创建学生账号。 */
    LoginResponse loginWithWechat(WechatLoginRequest request, String clientIp);

    /**
     * 创建普通用户账号并授予默认角色。
     *
     * @param request 注册资料
     * @return 新用户标识
     */
    RegisterResponse register(RegisterRequest request);

    /** 注销当前令牌对应的会话。 */
    void logout();

    /** 注销当前账号的全部会话。 */
    void logoutAll();

    /** 将微信身份绑定到当前账号。 */
    void bindWechat(String code);

    /** 解绑当前账号的微信身份；必须仍保留密码凭证。 */
    void unbindWechat();

    /** 为纯微信账号首次设置密码。 */
    void initializePassword(String newPassword);

    /**
     * 获取当前会话及实时角色、权限信息。
     *
     * @return 当前会话信息
     */
    SessionResponse currentSession();
}
