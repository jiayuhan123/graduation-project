package xyz.jiayuhan.userservice.model.dto;

import lombok.Data;

@Data
public class LoginDTO {
    /**
     * 账号标识 (手机号、用户名等)
     */
    private String identifier;

    /**
     * 密码或凭证
     */
    private String credential;
}
