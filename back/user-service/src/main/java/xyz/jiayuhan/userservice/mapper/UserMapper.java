package xyz.jiayuhan.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import xyz.jiayuhan.userservice.entity.User;

/** 用户账号 Mapper；逻辑删除规则由 {@link User} 实体注解统一应用。 */
public interface UserMapper extends BaseMapper<User> {
}
