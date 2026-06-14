package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台管理员/员工实体类 (B端用户)
 * 用于RBAC权限管理体系，支持双轨制身份绑定
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 后台账号主键ID，自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 后台系统的登录账号，必须全局唯一
     */
    private String username;

    /**
     * 后台登录密码 (需用 Security 的 BCrypt 强哈希算法加密存储)
     */
    private String password;

    /**
     * 后台员工的真实姓名或系统展示称呼
     */
    private String realName;

    /**
     * 后台员工的联系电话
     */
    private String phone;

    /**
     * 【核心字段】身份双轨制绑定关联的 C端 user_info.id
     * 如果该员工/老板同时也有小程序的消费者身份，绑定此ID后，可在小程序端开启管理入口
     */
    private Long bindUserId;

    /**
     * 后台账号状态
     * 1: 正常在职
     * 0: 禁用停用(离职等情况)
     */
    private Integer status;

    /**
     * 账号创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 账号信息最后修改时间
     */
    private LocalDateTime updatedAt;
}
