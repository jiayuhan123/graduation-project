package xyz.jiayuhan.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import xyz.jiayuhan.userservice.entity.Permission;

import java.util.List;

/** 权限 Mapper。 */
public interface PermissionMapper extends BaseMapper<Permission> {

    /** 查询用户通过所有已启用角色获得的去重权限编码。 */
    List<String> selectCodesByUserId(@Param("userId") Long userId);

    /** 查询指定角色关联的权限详情。 */
    List<Permission> selectByRoleId(@Param("roleId") Long roleId);
}
