package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 用户账号实体。
 *
 * <p>数据库 {@code password} 列只保存 BCrypt 哈希，任何接口响应都不得直接返回该实体。</p>
 */
@Data
@TableName("t_user")
public class User {

    /** 雪花算法生成的用户主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 规范化为小写的唯一登录用户名。 */
    private String username;

    /** 微信小程序唯一身份；只在服务端使用，绝不直接返回给客户端。 */
    private String wechatOpenid;

    /** BCrypt 密码哈希，对应历史数据库列 {@code password}。 */
    @TableField("password")
    private String passwordHash;

    /** 用户昵称。 */
    private String nickname;

    /** 真实姓名。 */
    private String realName;

    /** 规范化为小写的唯一邮箱。 */
    private String email;

    /** 唯一手机号。 */
    private String phone;

    /** 头像地址，对应历史数据库列 {@code avatar}。 */
    @TableField("avatar")
    private String avatarUrl;

    /** 账号状态：1 正常、0 禁用。 */
    private Integer status;

    /** 最近一次登录成功时间。 */
    private OffsetDateTime lastLoginAt;

    /** 最近一次登录成功的客户端 IP。 */
    private String lastLoginIp;

    /** 最近一次密码设置或修改时间。 */
    private OffsetDateTime passwordChangedAt;

    /** 创建时间，由 MyBatis-Plus 自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /** 最近更新时间，由 MyBatis-Plus 自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    /** 逻辑删除标记；常规查询会由 MyBatis-Plus 自动排除已删除数据。 */
    @TableField("is_deleted")
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;

    /** 乐观锁版本号，用于防止并发更新相互覆盖。 */
    @Version
    private Integer version;
}
