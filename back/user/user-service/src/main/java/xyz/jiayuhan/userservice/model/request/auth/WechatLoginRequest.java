package xyz.jiayuhan.userservice.model.request.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 微信小程序登录请求；年级和学期仅在首次自动创建学生账号时使用。 */
public record WechatLoginRequest(
        @NotBlank(message = "微信登录凭证不能为空")
        @Size(max = 256, message = "微信登录凭证过长")
        String code,
        @Size(max = 50, message = "昵称不能超过 50 个字符")
        String nickname,
        @Size(max = 50, message = "真实姓名不能超过 50 个字符")
        String realName,
        @Min(value = 1, message = "年级必须为 1-6")
        @Max(value = 6, message = "年级必须为 1-6")
        Integer gradeLevel,
        @Min(value = 1, message = "学期必须为 1 或 2")
        @Max(value = 2, message = "学期必须为 1 或 2")
        Integer semester
) {
}
