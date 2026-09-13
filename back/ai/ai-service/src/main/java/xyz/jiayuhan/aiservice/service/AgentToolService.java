package xyz.jiayuhan.aiservice.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.aiapi.port.AgentDomainToolPort;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 验证签名运行上下文并分派白名单工具。 */
@Service
public class AgentToolService {

    private final AgentTokenService tokenService;
    private final AgentDomainToolPort domainToolPort;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AgentToolService(
            AgentTokenService tokenService,
            AgentDomainToolPort domainToolPort,
            ObjectMapper objectMapper,
            Validator validator) {
        this.tokenService = tokenService;
        this.domainToolPort = domainToolPort;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public AgentContracts.ToolResult invoke(
            String token,
            AgentContracts.ToolInvocationRequest request) {
        AgentTokenService.ToolTokenPayload context = tokenService.verifyToolToken(token);
        if (!context.runId().equals(request.runId())
                || !context.allowedTools().contains(request.toolName())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前 Agent 运行无权调用该工具");
        }
        Object result = switch (request.toolName()) {
            case "get_class_dashboard" -> classDashboard(context.actorId(), request.arguments());
            case "search_questions" -> searchQuestions(request.arguments());
            case "create_assignment_draft" -> createAssignmentDraft(
                    context.actorId(), request.arguments());
            default -> throw new BusinessException(ResultCode.NOT_FOUND, "Agent 工具不存在");
        };
        return new AgentContracts.ToolResult(request.toolName(), result);
    }

    private AgentContracts.ClassDashboard classDashboard(Long actorId, Map<String, Object> arguments) {
        ClassDashboardArgs args = convert(arguments, ClassDashboardArgs.class);
        if (args.classId() == null || args.classId() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "classId 必须为正整数");
        }
        int days = args.days() == null ? 30 : args.days();
        if (days < 7 || days > 90) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "days 必须在7到90之间");
        }
        return domainToolPort.getClassDashboard(actorId, args.classId(), days);
    }

    private AgentContracts.QuestionPage searchQuestions(Map<String, Object> arguments) {
        SearchQuestionsArgs args = convert(arguments, SearchQuestionsArgs.class);
        long size = args.size() == null ? 10 : args.size();
        if (size < 1 || size > 30) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "size 必须在1到30之间");
        }
        if (args.difficulty() != null && (args.difficulty() < 1 || args.difficulty() > 5)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "difficulty 必须在1到5之间");
        }
        return domainToolPort.searchQuestions(
                1, size, args.keyword(), args.knowledgePointId(), args.questionType(), args.difficulty());
    }

    private AgentContracts.ActionDraft createAssignmentDraft(
            Long actorId,
            Map<String, Object> arguments) {
        AgentContracts.AssignmentDraftCommand command = convert(
                arguments, AgentContracts.AssignmentDraftCommand.class);
        validate(command);
        if (!command.dueAt().isAfter(command.startAt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "作业截止时间必须晚于开始时间");
        }
        AgentTokenService.ActionToken token = tokenService.issueActionToken(actorId, command);
        return new AgentContracts.ActionDraft(
                token.draftId(),
                "CREATE_ASSIGNMENT",
                command.title(),
                "为班级 " + command.classId() + " 创建 " + command.questions().size() + " 道题的作业草稿",
                token.expiresAt(),
                token.token());
    }

    private <T> T convert(Map<String, Object> arguments, Class<T> type) {
        try {
            return objectMapper.convertValue(arguments, type);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Agent 工具参数格式不正确");
        }
    }

    private <T> void validate(T value) {
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            throw new BusinessException(
                    ResultCode.BAD_REQUEST,
                    violations.iterator().next().getMessage());
        }
    }

    private record ClassDashboardArgs(Long classId, Integer days) {
    }

    private record SearchQuestionsArgs(
            String keyword,
            Long knowledgePointId,
            String questionType,
            Integer difficulty,
            Integer size
    ) {
    }
}
