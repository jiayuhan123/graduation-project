package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/** RBAC 角色实体，角色通过关联表聚合一组权限。 */
@Data
@TableName("sys_role")
public class Role {

    /** 数据库自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 程序鉴权使用的唯一角色编码，创建后保持稳定。 */
    private String code;

    /** 面向用户展示的角色名称。 */
    private String name;

    /** 角色用途说明。 */
    private String description;

    /** 1 启用、0 禁用；禁用角色不参与授权计算。 */
    private Integer status;

    /** 是否为系统内置角色；内置角色禁止禁用、改权和删除。 */
    @TableField("is_built_in")
    private Boolean builtIn;

    /** 创建时间，由 MyBatis-Plus 自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /** 最近更新时间，由 MyBatis-Plus 自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
