package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;

/**
 * 系统后台菜单与权限实体类
 * 控制后台前端左侧边栏显示，以及后端按钮级别的接口访问控制
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单主键ID，自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 父级菜单的ID
     * 顶级菜单默认为 0，实现无限极树状结构
     */
    private Long parentId;

    /**
     * 菜单标题/按钮名称
     * 例如：商品管理、订单发货
     */
    private String menuName;

    /**
     * 前端 Vue Router 路由地址路径
     */
    private String path;

    /**
     * 前端 Vue 组件所在的文件路径
     */
    private String component;

    /**
     * 后端接口权限校验标识符
     * 例如：'goods:list'，'order:send'
     */
    private String perms;

    /**
     * 权限类型分类
     * M: 目录 (Directory)
     * C: 菜单页面 (Component)
     * F: 按钮级别权限 (Function/Button)
     */
    private String menuType;

    /**
     * 菜单左侧显示的图标 class 或标识
     */
    private String icon;

    /**
     * 菜单在同级目录下的显示排序，数字越小越靠前
     */
    private Integer sortOrder;
}
