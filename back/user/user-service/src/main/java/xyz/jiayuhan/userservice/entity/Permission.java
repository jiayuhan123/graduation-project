package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * RBAC 权限字典实体。
 *
 * <p>权限是系统能力的稳定标识，由代码中的鉴权注解和 Flyway 迁移脚本共同维护，
 * 不提供运行时随意新增接口，避免数据库编码与程序鉴权点失配。</p>
 */
@Data
@TableName("sys_permission")
public class Permission {

    /** 自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 程序鉴权使用的唯一编码，例如 {@code system:user:list}。 */
    private String code;

    /** 面向管理端展示的权限名称。 */
    private String name;

    /** 权限用途说明。 */
    private String description;

    /** 创建时间，由 MyBatis-Plus 自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /** 最近更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
