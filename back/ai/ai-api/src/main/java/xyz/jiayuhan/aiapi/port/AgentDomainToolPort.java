package xyz.jiayuhan.aiapi.port;

import xyz.jiayuhan.aiapi.model.AgentContracts;

/** AI 模块访问领域能力的防腐层，由单体应用提供进程内实现。 */
public interface AgentDomainToolPort {

    AgentContracts.ClassDashboard getClassDashboard(Long actorId, Long classId, int days);

    AgentContracts.QuestionPage searchQuestions(
            long page,
            long size,
            String keyword,
            Long knowledgePointId,
            String questionType,
            Integer difficulty);

    AgentContracts.AssignmentCreated createAssignmentDraft(
            Long actorId,
            AgentContracts.AssignmentDraftCommand command);
}
