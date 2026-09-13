package xyz.jiayuhan.monomer.adapter;

import org.springframework.stereotype.Component;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.aiapi.port.AgentDomainToolPort;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.learningservice.service.ClassLearningAnalyticsService;
import xyz.jiayuhan.teachingservice.model.request.AssignmentQuestionRequest;
import xyz.jiayuhan.teachingservice.model.request.CreateAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.response.TeachingView;
import xyz.jiayuhan.teachingservice.service.TeachingCommandService;

import java.util.LinkedHashMap;
import java.util.Map;

/** 将 AI 防腐层契约映射到单体内现有学习与教学应用服务。 */
@Component
public class LocalAgentDomainToolPort implements AgentDomainToolPort {

    private final ClassLearningAnalyticsService learningAnalyticsService;
    private final TeachingCommandService teachingCommandService;

    public LocalAgentDomainToolPort(
            ClassLearningAnalyticsService learningAnalyticsService,
            TeachingCommandService teachingCommandService) {
        this.learningAnalyticsService = learningAnalyticsService;
        this.teachingCommandService = teachingCommandService;
    }

    @Override
    public AgentContracts.ClassDashboard getClassDashboard(Long actorId, Long classId, int days) {
        LearningView.ClassDashboard source = learningAnalyticsService.getDashboard(actorId, classId, days);
        Map<Long, String> studentAliases = new LinkedHashMap<>();
        return new AgentContracts.ClassDashboard(
                source.classId(), source.className(), source.schoolName(), source.gradeLevel(),
                source.semester(), source.rangeDays(), source.rangeStart(), source.rangeEnd(),
                new AgentContracts.ClassMetrics(
                        source.metrics().studentCount(), source.metrics().taskCount(),
                        source.metrics().completedTaskCount(), source.metrics().completionRate(),
                        source.metrics().answerCount(), source.metrics().correctAnswerCount(),
                        source.metrics().accuracyRate(), source.metrics().openErrorCount()),
                source.weakKnowledgePoints().stream()
                        .map(item -> new AgentContracts.WeakKnowledgePoint(
                                item.knowledgePointId(), item.knowledgePointName(),
                                item.averageMastery(), item.averageAccuracy(),
                                item.affectedStudentCount(), item.assessedStudentCount()))
                        .toList(),
                source.frequentWrongQuestions().stream()
                        .map(item -> new AgentContracts.FrequentWrongQuestion(
                                item.questionId(), item.questionCode(), item.stem(),
                                item.errorCount(), item.affectedStudentCount()))
                        .toList(),
                source.frequentErrorTypes().stream()
                        .map(item -> new AgentContracts.FrequentErrorType(
                                item.errorTypeId(), item.errorTypeName(), item.category(),
                                item.errorCount(), item.affectedStudentCount()))
                        .toList(),
                source.dailyTrends().stream()
                        .map(item -> new AgentContracts.DailyTrend(
                                item.date(), item.answerCount(), item.correctAnswerCount(),
                                item.accuracyRate()))
                        .toList(),
                source.alerts().stream()
                        .map(item -> new AgentContracts.StudentAlert(
                                null,
                                studentAliases.computeIfAbsent(
                                        item.studentId(),
                                        ignored -> "需关注学生-" + (studentAliases.size() + 1)),
                                item.code(),
                                item.message(), item.severity()))
                        .toList());
    }

    @Override
    public AgentContracts.QuestionPage searchQuestions(
            long page,
            long size,
            String keyword,
            Long knowledgePointId,
            String questionType,
            Integer difficulty) {
        PageResult<TeachingView.QuestionCandidate> source = teachingCommandService.pageQuestionCandidates(
                page, size, keyword, knowledgePointId, questionType, difficulty);
        return new AgentContracts.QuestionPage(
                source.page(), source.size(), source.total(), source.pages(),
                source.records().stream()
                        .map(item -> new AgentContracts.QuestionCandidate(
                                item.id(), item.code(), item.questionType(), item.stem(),
                                item.difficulty(), item.estimatedSeconds()))
                        .toList());
    }

    @Override
    public AgentContracts.AssignmentCreated createAssignmentDraft(
            Long actorId,
            AgentContracts.AssignmentDraftCommand command) {
        CreateAssignmentRequest request = new CreateAssignmentRequest(
                command.classId(), command.title(), command.description(), command.assignmentType(),
                command.startAt(), command.dueAt(), command.settings(),
                command.questions().stream()
                        .map(question -> new AssignmentQuestionRequest(
                                question.questionId(), question.layerCode(),
                                question.sequenceNo(), question.score()))
                        .toList());
        TeachingView.AssignmentDetail created = teachingCommandService.createAssignment(actorId, request);
        return new AgentContracts.AssignmentCreated(
                created.assignment().id(), created.assignment().classId(),
                created.assignment().title(), created.assignment().status(),
                created.assignment().version());
    }
}
