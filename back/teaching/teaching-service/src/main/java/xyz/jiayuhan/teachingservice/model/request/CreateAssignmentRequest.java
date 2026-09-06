package xyz.jiayuhan.teachingservice.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/** 创建作业草稿及选题。 */
public record CreateAssignmentRequest(
        @NotNull(message = "班级编号不能为空") Long classId,
        @NotBlank(message = "作业标题不能为空") @Size(max = 128) String title,
        @Size(max = 1000, message = "作业说明不能超过1000个字符") String description,
        @NotBlank(message = "作业类型不能为空")
        @Pattern(regexp = "UNIFIED|LAYERED", message = "作业类型不正确") String assignmentType,
        @NotNull(message = "开始时间不能为空") OffsetDateTime startAt,
        @NotNull(message = "截止时间不能为空") OffsetDateTime dueAt,
        String settings,
        @NotEmpty(message = "作业至少包含一道题")
        @Size(max = 100, message = "一份作业最多100道题") List<@Valid AssignmentQuestionRequest> questions
) {
}
