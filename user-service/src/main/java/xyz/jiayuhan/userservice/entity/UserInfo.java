package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户基础信息实体类 (C端消费者)
 * 代表系统中的一个物理自然人
 */
@Data
@TableName("user_info")
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户全局主键ID
     * 采用 MyBatis-Plus 内置的雪花算法生成，保障分布式环境下的唯一性
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户展示昵称
     * 默认可以为 '普通用户' 或是 微信授权获取的昵称
     */
    private String nickname;

    /**
     * 用户头像URL
     * 关联对象存储(MinIO)中的外部链接或文件路径
     */
    private String avatar;

    /**
     * 用户性别
     * 0: 未知 (默认)
     * 1: 男性
     * 2: 女性
     */
    private Integer gender;

    /**
     * 账户状态
     * 1: 正常可用
     * 0: 已被封禁禁用 (黑名单机制)
     */
    private Integer status;

    /**
     * 账户首次注册时间
     * 数据库默认设置 CURRENT_TIMESTAMP
     */
    private LocalDateTime createdAt;

    /**
     * 账户信息最后更新时间
     * 数据库默认 ON UPDATE CURRENT_TIMESTAMP，自动维护
     */
    private LocalDateTime updatedAt;
}
