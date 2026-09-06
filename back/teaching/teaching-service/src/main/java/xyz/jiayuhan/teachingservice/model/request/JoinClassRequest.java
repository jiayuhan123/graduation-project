package xyz.jiayuhan.teachingservice.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 学生凭班级码加入班级。 */
public record JoinClassRequest(
        @NotBlank(message = "班级码不能为空")
        @Pattern(regexp = "^[A-Za-z0-9]{6,12}$", message = "班级码格式不正确") String classCode,
        @Size(max = 32, message = "班内学号不能超过32个字符") String studentNoInClass
) {
}
