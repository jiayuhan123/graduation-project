package xyz.jiayuhan.contentservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.contentservice.entity.Question;
import xyz.jiayuhan.contentservice.entity.QuestionFeedback;
import xyz.jiayuhan.contentservice.event.ContentAuditActions;
import xyz.jiayuhan.contentservice.event.ContentEventOutboxService;
import xyz.jiayuhan.contentservice.mapper.QuestionFeedbackMapper;
import xyz.jiayuhan.contentservice.mapper.QuestionMapper;
import xyz.jiayuhan.contentservice.model.request.HandleQuestionFeedbackRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionFeedbackServiceImplTest {

    @Mock private QuestionFeedbackMapper questionFeedbackMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private ContentEventOutboxService outboxService;
    @InjectMocks private QuestionFeedbackServiceImpl service;

    @Test
    void resolvesFeedbackAndDisablesEnabledQuestion() {
        QuestionFeedback feedback = new QuestionFeedback();
        feedback.setId(8L);
        feedback.setQuestionId(10L);
        feedback.setReporterId(20L);
        feedback.setCategory("ANSWER_ERROR");
        feedback.setDescription("标准答案应为 12");
        feedback.setStatus("PENDING");
        feedback.setVersion(0);
        Question question = new Question();
        question.setId(10L);
        question.setCode("Q-10");
        question.setStem("5 + 7 = ?");
        question.setStatus("ENABLED");
        when(questionFeedbackMapper.selectById(8L)).thenReturn(feedback);
        when(questionMapper.selectById(10L)).thenReturn(question);
        when(questionMapper.updateById(question)).thenReturn(1);
        when(questionFeedbackMapper.updateById(feedback)).thenReturn(1);

        var result = service.handle(
                99L, 8L, new HandleQuestionFeedbackRequest(
                        "RESOLVE_AND_DISABLE", "答案待复核，先下架", 0));

        assertThat(result.status()).isEqualTo("RESOLVED");
        assertThat(result.handledAt()).isNotNull();
        assertThat(question.getStatus()).isEqualTo("DISABLED");
        assertThat(question.getReviewedBy()).isEqualTo(99L);
        verify(outboxService).appendAudit(
                99L, ContentAuditActions.QUESTION_FEEDBACK_HANDLED,
                "QUESTION_FEEDBACK", 8L, "action=RESOLVE_AND_DISABLE,questionId=10");
    }
}
