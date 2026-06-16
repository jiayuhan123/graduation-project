package xyz.jiayuhan.userservice.model.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    /**
     * 账号标识 (手机号、用户名等)
     */
    private String identifier;

    /**
     * 密码或凭证
     */
    private String credential;

    /**
     * 用户昵称（可选）
     */
    private String nickname;
}
