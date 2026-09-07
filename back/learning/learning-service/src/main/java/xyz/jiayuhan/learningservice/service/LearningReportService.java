package xyz.jiayuhan.learningservice.service;

import xyz.jiayuhan.learningservice.model.response.LearningView;

/** 当前学生自查及教师/管理员跨学生学情查询。 */
public interface LearningReportService {

    LearningView.StudentReport getOwnReport(Long userId);

    LearningView.StudentReport getStudentReport(Long requesterId, Long studentId);
}
