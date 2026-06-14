package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统后台角色实体类
 * 用于区分后台员工的不同职级与权限组
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色主键ID，自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色中文名称
     * 例如：超级管理员、普通店长、在线客服
     */
    private String roleName;

    /**
     * 角色权限安全编码
     * 供后端鉴权框架 (如 Sa-Token) 使用，例如：ADMIN, SHOP_MANAGER
     */
    private String roleCode;

    /**
     * 角色启用状态
     * 1: 正常启用
     * 0: 停用作废
     */
    private Integer status;

    /**
     * 角色创建时间
     */
    private LocalDateTime createdAt;
}
