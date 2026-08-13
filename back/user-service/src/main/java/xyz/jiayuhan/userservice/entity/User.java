package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 居民用户实体类（对应 t_user 表）
 */
@Data
@TableName("t_user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户主键 ID（MyBatis-Plus 雪花算法生成）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 登录用户名（全局唯一）
     */
    private String username;

    /**
     * 登录密码（BCrypt 加密存储）
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号（登录凭证之一，全局唯一）
     */
    private String phone;

    /**
     * 头像链接（file-service 上传后返回）
     */
    private String avatar;

    /**
     * 账户状态：1 正常，0 禁用
     */
    private Integer status;

    /**
     * 注册时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
}
