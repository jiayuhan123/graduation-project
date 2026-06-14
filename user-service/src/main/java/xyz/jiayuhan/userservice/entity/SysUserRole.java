package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;

/**
 * 后台管理员与角色的中间关联实体类
 * 典型的多对多关系表映射
 */
@Data
@TableName("sys_user_role")
public class SysUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联的后台管理员/员工 ID (对应 sys_user 表的 id)
     */
    private Long userId;

    /**
     * 赋予该员工的角色 ID (对应 sys_role 表的 id)
     */
    private Long roleId;
}
