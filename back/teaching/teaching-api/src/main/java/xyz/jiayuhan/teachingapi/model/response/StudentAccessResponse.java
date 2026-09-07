package xyz.jiayuhan.teachingapi.model.response;

/** 教师是否通过有效班级关系有权查看一个学生的学情。 */
public record StudentAccessResponse(
        Long teacherId,
        Long studentId,
        boolean accessible
) {
}
