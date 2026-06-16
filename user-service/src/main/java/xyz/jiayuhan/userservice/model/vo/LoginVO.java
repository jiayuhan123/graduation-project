package xyz.jiayuhan.userservice.model.vo;

import lombok.Data;

@Data
public class LoginVO {
    /**
     * 登录 Token
     */
    private String token;

    /**
     * 用户全局主键 ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 头像链接
     */
    private String avatar;
}
