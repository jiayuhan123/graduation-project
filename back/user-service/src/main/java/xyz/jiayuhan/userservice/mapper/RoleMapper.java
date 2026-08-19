package xyz.jiayuhan.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import xyz.jiayuhan.userservice.entity.Role;

import java.util.List;

/** 角色 Mapper。 */
public interface RoleMapper extends BaseMapper<Role> {

    /** 查询用户当前全部已启用角色编码。 */
    List<String> selectCodesByUserId(@Param("userId") Long userId);

    /** 查询用户当前全部已启用角色实体。 */
    List<Role> selectEnabledByUserId(@Param("userId") Long userId);
}
