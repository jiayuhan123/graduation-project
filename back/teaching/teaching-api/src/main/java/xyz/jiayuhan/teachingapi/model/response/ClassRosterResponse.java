package xyz.jiayuhan.teachingapi.model.response;

import java.util.List;

/** 供学习分析使用的班级最小花名册，不跨服务暴露成员关系实体。 */
public record ClassRosterResponse(
        Long classId,
        String className,
        String schoolName,
        Integer gradeLevel,
        Integer semester,
        String status,
        List<Long> studentIds
) {
}
