package xyz.jiayuhan.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.jiayuhan.userservice.entity.UserAuth;

@Mapper
public interface UserAuthMapper extends BaseMapper<UserAuth> {
}
