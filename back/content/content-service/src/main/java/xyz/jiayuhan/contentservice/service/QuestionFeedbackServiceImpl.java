package xyz.jiayuhan.contentservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.contentservice.entity.Question;
import xyz.jiayuhan.contentservice.entity.QuestionFeedback;
import xyz.jiayuhan.contentservice.event.ContentAuditActions;
import xyz.jiayuhan.contentservice.event.ContentEventOutboxService;
import xyz.jiayuhan.contentservice.mapper.QuestionFeedbackMapper;
import xyz.jiayuhan.contentservice.mapper.QuestionMapper;
import xyz.jiayuhan.contentservice.model.request.CreateQuestionFeedbackRequest;
import xyz.jiayuhan.contentservice.model.request.HandleQuestionFeedbackRequest;
import xyz.jiayuhan.contentservice.model.response.QuestionFeedbackResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** 每个用户对同一道题只保留一条未完结反馈，处理动作全部进入内容审计。 */
@Service
@RequiredArgsConstructor
public class QuestionFeedbackServiceImpl implements QuestionFeedbackService {

    private final QuestionFeedbackMapper questionFeedbackMapper;
    private final QuestionMapper questionMapper;
    private final ContentEventOutboxService outboxService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionFeedbackResponse create(
            Long reporterId, Long questionId, CreateQuestionFeedbackRequest request) {
        Question question = requireQuestion(questionId);
        QuestionFeedback feedback = new QuestionFeedback();
        feedback.setQuestionId(questionId);
        feedback.setReporterId(reporterId);
        feedback.setCategory(request.category());
        feedback.setDescription(request.description().trim());
        feedback.setStatus("PENDING");
        feedback.setVersion(0);
        try {
            questionFeedbackMapper.insert(feedback);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.CONFLICT, "你已提交过尚未处理的题目反馈");
        }
        outboxService.appendAudit(
                reporterId, ContentAuditActions.QUESTION_FEEDBACK_CREATED,
                "QUESTION_FEEDBACK", feedback.getId(), "questionId=" + questionId);
        return toView(feedback, question);
    }

    @Override
    public PageResult<QuestionFeedbackResponse> page(
            long page, long size, String status, String category, Long questionId) {
        Page<QuestionFeedback> result = questionFeedbackMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<QuestionFeedback>()
                        .eq(StringUtils.hasText(status), QuestionFeedback::getStatus, status)
                        .eq(StringUtils.hasText(category), QuestionFeedback::getCategory, category)
                        .eq(questionId != null, QuestionFeedback::getQuestionId, questionId)
                        .orderByAsc(QuestionFeedback::getStatus)
                        .orderByDesc(QuestionFeedback::getCreatedAt)
                        .orderByDesc(QuestionFeedback::getId));
        List<QuestionFeedbackResponse> records = result.getRecords().stream()
                .map(feedback -> toView(feedback, questionMapper.selectById(feedback.getQuestionId())))
                .toList();
        return new PageResult<>(
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionFeedbackResponse handle(
            Long handlerId, Long feedbackId, HandleQuestionFeedbackRequest request) {
        QuestionFeedback feedback = questionFeedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目反馈不存在");
        }
        if (!request.version().equals(feedback.getVersion())) {
            throw new BusinessException(ResultCode.CONFLICT, "反馈已被其他管理员处理，请刷新后重试");
        }
        if (List.of("RESOLVED", "REJECTED").contains(feedback.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "反馈已经处理完成");
        }
        Question question = requireQuestion(feedback.getQuestionId());
        OffsetDateTime handledAt = OffsetDateTime.now(ZoneOffset.UTC);
        String nextStatus = switch (request.action()) {
            case "PROCESS" -> {
                if (!"PENDING".equals(feedback.getStatus())) {
                    throw new BusinessException(ResultCode.CONFLICT, "反馈已经开始处理");
                }
                yield "PROCESSING";
            }
            case "RESOLVE", "RESOLVE_AND_DISABLE" -> "RESOLVED";
            case "REJECT" -> "REJECTED";
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "处理动作不正确");
        };
        if ("RESOLVE_AND_DISABLE".equals(request.action())
                && "ENABLED".equals(question.getStatus())) {
            question.setStatus("DISABLED");
            question.setReviewedBy(handlerId);
            question.setReviewedAt(handledAt);
            if (questionMapper.updateById(question) != 1) {
                throw new BusinessException(ResultCode.CONTENT_VERSION_CONFLICT);
            }
        }
        feedback.setStatus(nextStatus);
        feedback.setHandlerId(handlerId);
        feedback.setHandleComment(trimToNull(request.comment()));
        feedback.setHandledAt("PROCESSING".equals(nextStatus) ? null : handledAt);
        if (questionFeedbackMapper.updateById(feedback) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "反馈已被其他管理员处理，请刷新后重试");
        }
        outboxService.appendAudit(
                handlerId, ContentAuditActions.QUESTION_FEEDBACK_HANDLED,
                "QUESTION_FEEDBACK", feedbackId,
                "action=" + request.action() + ",questionId=" + feedback.getQuestionId());
        return toView(feedback, question);
    }

    private Question requireQuestion(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.QUESTION_NOT_FOUND);
        }
        return question;
    }

    private QuestionFeedbackResponse toView(QuestionFeedback feedback, Question question) {
        return new QuestionFeedbackResponse(
                feedback.getId(), feedback.getQuestionId(),
                question == null ? null : question.getCode(),
                question == null ? null : question.getStem(),
                question == null ? null : question.getStatus(),
                feedback.getReporterId(), feedback.getCategory(), feedback.getDescription(),
                feedback.getStatus(), feedback.getHandlerId(), feedback.getHandleComment(),
                feedback.getHandledAt(), feedback.getCreatedAt(), feedback.getUpdatedAt(),
                feedback.getVersion());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
