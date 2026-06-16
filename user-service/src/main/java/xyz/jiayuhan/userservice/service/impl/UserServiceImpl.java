package xyz.jiayuhan.userservice.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import xyz.jiayuhan.userservice.entity.UserAuth;
import xyz.jiayuhan.userservice.entity.UserInfo;
import xyz.jiayuhan.userservice.mapper.UserAuthMapper;
import xyz.jiayuhan.userservice.mapper.UserInfoMapper;
import xyz.jiayuhan.userservice.model.dto.LoginDTO;
import xyz.jiayuhan.userservice.model.vo.LoginVO;
import xyz.jiayuhan.userservice.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserAuthMapper userAuthMapper;
    private final UserInfoMapper userInfoMapper;

    public UserServiceImpl(UserAuthMapper userAuthMapper, UserInfoMapper userInfoMapper) {
        this.userAuthMapper = userAuthMapper;
        this.userInfoMapper = userInfoMapper;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 1. 根据登录凭证查询 user_auth 表
        // 此处为了简化，假定身份类型 password 对应的 identifier 就是前端传来的账号
        UserAuth userAuth = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getIdentityType, "password")
                .eq(UserAuth::getIdentifier, loginDTO.getIdentifier()));

        if (userAuth == null) {
            throw new RuntimeException("账号不存在");
        }

        // 2. 校验密码
        // 注意：生产环境中，数据库密码应为 BCrypt 加密后的密文，这里需要使用 BCrypt.checkpw 进行比对
        // 为了演示或测试方便，如果你数据库存的是明文，可以直接 equals 比较
        if (!userAuth.getCredential().equals(loginDTO.getCredential())) {
            throw new RuntimeException("密码错误");
        }

        // 3. 获取用户信息
        UserInfo userInfo = userInfoMapper.selectById(userAuth.getUserId());
        if (userInfo == null) {
            throw new RuntimeException("用户数据异常");
        }
        if (userInfo.getStatus() == 0) {
            throw new RuntimeException("该账号已被禁用");
        }

        // 4. 执行 Sa-Token 登录操作，底层会自动生成 Token 存入 Redis
        StpUtil.login(userInfo.getId());

        // 5. 封装返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(StpUtil.getTokenValue());
        loginVO.setUserId(userInfo.getId());
        loginVO.setNickname(userInfo.getNickname());
        loginVO.setAvatar(userInfo.getAvatar());

        return loginVO;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void register(xyz.jiayuhan.userservice.model.dto.RegisterDTO registerDTO) {
        // 1. 检查账号是否已存在
        Long count = userAuthMapper.selectCount(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getIdentityType, "password")
                .eq(UserAuth::getIdentifier, registerDTO.getIdentifier()));
        
        if (count != null && count > 0) {
            throw new RuntimeException("该账号已被注册");
        }

        // 2. 插入基础用户信息 (UserInfo)
        UserInfo userInfo = new UserInfo();
        // 如果用户没填昵称，默认使用账号名
        userInfo.setNickname(
            org.springframework.util.StringUtils.hasText(registerDTO.getNickname()) 
            ? registerDTO.getNickname() 
            : registerDTO.getIdentifier()
        );
        userInfo.setAvatar(""); // 默认空头像
        userInfo.setGender(0); // 默认未知
        userInfo.setStatus(1); // 正常状态
        userInfoMapper.insert(userInfo);

        // 3. 插入账号授权信息 (UserAuth)
        UserAuth userAuth = new UserAuth();
        userAuth.setUserId(userInfo.getId()); // UserInfo 插入后会自动回填生成的 ID
        userAuth.setIdentityType("password");
        userAuth.setIdentifier(registerDTO.getIdentifier());
        // 注意：生产环境中这里一定要对密码进行加密（如 BCrypt），这里暂存明文方便你演示
        userAuth.setCredential(registerDTO.getCredential());
        userAuthMapper.insert(userAuth);
    }
}
