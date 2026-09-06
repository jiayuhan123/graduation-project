package xyz.jiayuhan.userservice.integration.wechat;

/** 网页扫码开始时保存的首次注册资料，授权回调成功后一次性消费。 */
public record WechatWebLoginContext(
        String nickname,
        String realName,
        Integer gradeLevel,
        Integer semester
) {
}
