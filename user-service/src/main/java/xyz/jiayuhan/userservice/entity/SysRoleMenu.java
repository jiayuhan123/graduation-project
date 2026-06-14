package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;

/**
 * 角色与菜单权限的中间关联实体类
 * 定义某种角色具体拥有哪些页面查看权和按钮操作权
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 被授权的角色 ID (对应 sys_role 表的 id)
     */
    private Long roleId;

    /**
     * 授予给该角色的 菜单/页面/按钮 ID (对应 sys_menu 表的 id)
     */
    private Long menuId;
}
