package xyz.jiayuhan.teachingservice.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 教师添加学生到本人管理的班级。 */
public record AddClassStudentRequest(
        @NotNull(message = "学生编号不能为空") Long studentId,
        @Size(max = 32, message = "班内学号不能超过32个字符") String studentNoInClass
) {
}
