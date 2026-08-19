package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户与角色的多对多关联实体，二者共同组成数据库联合主键。 */
@Data
@TableName("sys_user_role")
public class UserRole {

    /** 用户 ID。 */
    private Long userId;

    /** 角色 ID。 */
    private Long roleId;

    /** 关联创建时间，由 MyBatis-Plus 自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
