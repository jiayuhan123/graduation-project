package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 角色与权限的多对多关联实体，二者共同组成数据库联合主键。 */
@Data
@TableName("sys_role_permission")
public class RolePermission {

    /** 角色 ID。 */
    private Long roleId;

    /** 权限 ID。 */
    private Long permissionId;

    /** 关联创建时间，由 MyBatis-Plus 自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
