package xyz.jiayuhan.userservice.model.response.account;

import java.time.OffsetDateTime;

/** 学生资料安全响应。 */
public record StudentProfileResponse(
        Long userId,
        String studentNo,
        Integer gradeLevel,
        Integer semester,
        OffsetDateTime updatedAt
) {
}
