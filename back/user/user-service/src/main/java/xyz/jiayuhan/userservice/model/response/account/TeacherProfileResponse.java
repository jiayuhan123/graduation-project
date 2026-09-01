package xyz.jiayuhan.userservice.model.response.account;

import java.time.OffsetDateTime;

/** 教师资料安全响应。 */
public record TeacherProfileResponse(
        Long userId,
        String teacherNo,
        String title,
        String introduction,
        OffsetDateTime updatedAt
) {
}
