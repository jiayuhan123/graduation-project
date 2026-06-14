package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户收货地址实体类
 * 用于存储标准商城模式下的物流收货地址
 */
@Data
@TableName("user_address")
public class UserAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联 user_info 表的主键 ID
     * 表明该地址属于哪位用户
     */
    private Long userId;

    /**
     * 收货人真实姓名
     */
    private String recipientName;

    /**
     * 收货人联系电话
     */
    private String phone;

    /**
     * 所在省市区信息
     * 例如：辽宁省 大连市 甘井子区
     */
    private String region;

    /**
     * 详细收货地址
     * 例如：XX小区2栋3单元301室
     */
    private String detailAddress;

    /**
     * 是否设置为默认收货地址
     * 1: 是默认地址
     * 0: 非默认地址
     */
    private Integer isDefault;

    /**
     * 该地址条目的创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 该地址条目的最后修改时间
     */
    private LocalDateTime updatedAt;
}
