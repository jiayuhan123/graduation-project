package xyz.jiayuhan.userapi.model.response;

/** 服务间校验用户存在性、状态和角色的结果。 */
public record UserValidationResponse(
        Long userId,
        boolean exists,
        boolean active,
        boolean roleMatched
) {
}
