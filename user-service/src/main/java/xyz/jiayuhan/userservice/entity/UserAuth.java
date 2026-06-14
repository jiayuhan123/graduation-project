package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 多端用户授权登录实体类
 * 一名用户可以拥有多种登录方式(手机号验证码、微信小程序、账号密码等)
 */
@Data
@TableName("user_auth")
public class UserAuth implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联 user_info 表的主键 ID
     * 所有的登录凭证最终都指向同一个自然人
     */
    private Long userId;

    /**
     * 登录凭证类型
     * 枚举值例如: 
     * 'phone' - 手机号登录
     * 'wechat_mini' - 微信小程序登录
     * 'password' - 账号密码登录
     */
    private String identityType;

    /**
     * 唯一标识符
     * 例如具体的手机号、或者微信官方返回的 OpenID
     */
    private String identifier;

    /**
     * 安全凭证
     * 对于密码登录，存储加密后的 hash 值
     * 对于微信小程序登录，此字段可留空，因为验证交给微信底层
     */
    private String credential;

    /**
     * 该授权方式绑定的时间
     */
    private LocalDateTime createdAt;
}
