package xyz.jiayuhan.teachingapi.model.response;

/** 教师对指定班级的管理关系校验结果。 */
public record ClassAccessResponse(
        Long teacherId,
        Long classId,
        boolean accessible
) {
}
