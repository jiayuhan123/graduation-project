package xyz.jiayuhan.userservice.model.request.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 发起网页微信扫码登录；年级和学期仅在首次自动创建学生账号时使用。 */
public record WechatWebAuthorizeRequest(
        @Size(max = 50, message = "昵称长度不能超过 50 个字符") String nickname,
        @Size(max = 50, message = "真实姓名长度不能超过 50 个字符") String realName,
        @NotNull(message = "请选择年级") @Min(1) @Max(6) Integer gradeLevel,
        @NotNull(message = "请选择学期") @Min(1) @Max(2) Integer semester
) {
}
