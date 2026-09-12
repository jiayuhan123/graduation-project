package xyz.jiayuhan.userapi.model.response;

import java.util.List;

/** 供其他领域模块使用的最小用户目录信息，不包含联系方式与微信身份。 */
public record UserSummaryResponse(
        Long id,
        String username,
        String nickname,
        String realName,
        String avatarUrl,
        Integer status,
        List<String> roles,
        Integer gradeLevel,
        Integer semester
) {
}
