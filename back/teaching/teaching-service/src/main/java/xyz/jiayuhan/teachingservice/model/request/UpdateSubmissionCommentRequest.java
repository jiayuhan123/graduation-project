package xyz.jiayuhan.teachingservice.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 教师修改作业提交评语。 */
public record UpdateSubmissionCommentRequest(
        @Size(max = 500, message = "教师评语不能超过500个字符") String teacherComment,
        @NotNull(message = "版本号不能为空") @Min(0) Integer version
) {
}
