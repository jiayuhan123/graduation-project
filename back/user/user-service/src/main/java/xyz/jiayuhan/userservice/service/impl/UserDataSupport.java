package xyz.jiayuhan.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.userservice.entity.User;
import xyz.jiayuhan.userservice.mapper.UserMapper;

import java.util.Locale;

/** 用户字段规范化与唯一性校验的共享实现。 */
@Component
@RequiredArgsConstructor
public class UserDataSupport {

    private final UserMapper userMapper;

    /** 将用户名去除首尾空白并按稳定区域规则转换为小写。 */
    public String normalizeUsername(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /** 将邮箱规范化为小写；空白输入统一转换为 {@code null}。 */
    public String normalizeEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    /** 去除首尾空白，并把空字符串统一转换为 {@code null}。 */
    public String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 校验用户名、手机号和邮箱在未删除用户中的唯一性。
     *
     * @param username 待校验用户名，可为空
     * @param phone 待校验手机号，可为空
     * @param email 待校验邮箱，可为空
     * @param excludedUserId 编辑场景中需要排除的当前用户 ID，可为空
     */
    public void checkUnique(String username, String phone, String email, Long excludedUserId) {
        if (username != null && exists(User::getUsername, username, excludedUserId)) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        if (phone != null && exists(User::getPhone, phone, excludedUserId)) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }
        if (email != null && exists(User::getEmail, email, excludedUserId)) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS);
        }
    }

    private boolean exists(com.baomidou.mybatisplus.core.toolkit.support.SFunction<User, ?> column,
                           String value,
                           Long excludedUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().eq(column, value);
        if (excludedUserId != null) {
            wrapper.ne(User::getId, excludedUserId);
        }
        return userMapper.exists(wrapper);
    }
}
