package xyz.jiayuhan.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.userservice.entity.User;
import xyz.jiayuhan.userservice.mapper.UserMapper;
import xyz.jiayuhan.userservice.model.response.account.StudentProfileResponse;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;
import xyz.jiayuhan.userservice.service.ProfileService;
import xyz.jiayuhan.userservice.service.RbacService;
import xyz.jiayuhan.userservice.service.UserDirectoryService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 内部用户目录实现，响应严格排除邮箱、手机号、OpenID 和登录凭证。 */
@Service
@RequiredArgsConstructor
public class UserDirectoryServiceImpl implements UserDirectoryService {

    private final UserMapper userMapper;
    private final RbacService rbacService;
    private final ProfileService profileService;

    @Override
    public UserSummaryResponse summary(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toSummary(user);
    }

    @Override
    public List<UserSummaryResponse> summaries(List<Long> userIds) {
        List<Long> distinctIds = userIds.stream().distinct().toList();
        Map<Long, User> users = userMapper.selectByIds(distinctIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return distinctIds.stream()
                .map(users::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toSummary)
                .toList();
    }

    @Override
    public UserValidationResponse validate(Long userId, String roleCode) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return new UserValidationResponse(userId, false, false, false);
        }
        boolean active = Integer.valueOf(1).equals(user.getStatus());
        boolean roleMatched = roleCode == null || rbacService.roleCodes(userId).contains(roleCode);
        return new UserValidationResponse(userId, true, active, roleMatched);
    }

    private UserSummaryResponse toSummary(User user) {
        StudentProfileResponse student = profileService.studentProfile(user.getId());
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRealName(),
                user.getAvatarUrl(),
                user.getStatus(),
                rbacService.roleCodes(user.getId()),
                student == null ? null : student.gradeLevel(),
                student == null ? null : student.semester());
    }
}
