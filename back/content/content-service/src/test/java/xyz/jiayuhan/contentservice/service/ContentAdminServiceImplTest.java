package xyz.jiayuhan.contentservice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.contentservice.entity.ContentModule;
import xyz.jiayuhan.contentservice.entity.KnowledgePoint;
import xyz.jiayuhan.contentservice.entity.Question;
import xyz.jiayuhan.contentservice.entity.QuestionKnowledgePoint;
import xyz.jiayuhan.contentservice.event.ContentAuditActions;
import xyz.jiayuhan.contentservice.event.ContentEventOutboxService;
import xyz.jiayuhan.contentservice.mapper.ContentModuleMapper;
import xyz.jiayuhan.contentservice.mapper.ErrorTypeMapper;
import xyz.jiayuhan.contentservice.mapper.KnowledgePointMapper;
import xyz.jiayuhan.contentservice.mapper.QuestionKnowledgePointMapper;
import xyz.jiayuhan.contentservice.mapper.QuestionMapper;
import xyz.jiayuhan.contentservice.model.request.ReviewQuestionRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证题目审核状态机和发布前关系校验。 */
@ExtendWith(MockitoExtension.class)
class ContentAdminServiceImplTest {

    @Mock
    private ContentModuleMapper contentModuleMapper;
    @Mock
    private KnowledgePointMapper knowledgePointMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private QuestionKnowledgePointMapper questionKnowledgePointMapper;
    @Mock
    private ErrorTypeMapper errorTypeMapper;
    @Mock
    private ContentEventOutboxService outboxService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ContentAdminServiceImpl contentAdminService;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void approvesDraftWhenBoundKnowledgePointAndModuleAreEnabled() {
        UserContext.setUserId(42L);
        Question question = question("DRAFT");
        QuestionKnowledgePoint binding = binding();
        KnowledgePoint knowledgePoint = enabledKnowledgePoint();
        ContentModule module = enabledModule();
        when(questionMapper.selectById(100L)).thenReturn(question);
        when(questionKnowledgePointMapper.selectList(any()))
                .thenReturn(List.of(binding), List.of(binding));
        when(knowledgePointMapper.selectById(20L)).thenReturn(knowledgePoint);
        when(knowledgePointMapper.selectByIds(List.of(20L))).thenReturn(List.of(knowledgePoint));
        when(contentModuleMapper.selectById(10L)).thenReturn(module);
        when(questionMapper.updateById(question)).thenReturn(1);

        var response = contentAdminService.reviewQuestion(
                100L, new ReviewQuestionRequest("APPROVE", "答案已核对", 0));

        assertThat(response.status()).isEqualTo("ENABLED");
        assertThat(response.reviewedBy()).isEqualTo(42L);
        assertThat(response.reviewedAt()).isNotNull();
        verify(outboxService).appendAudit(
                42L,
                ContentAuditActions.QUESTION_REVIEWED,
                "QUESTION",
                100L,
                "decision=APPROVE,remark=答案已核对");
    }

    @Test
    void rejectsReturnDecisionForPublishedQuestion() {
        Question question = question("ENABLED");
        when(questionMapper.selectById(100L)).thenReturn(question);

        assertThatThrownBy(() -> contentAdminService.reviewQuestion(
                100L, new ReviewQuestionRequest("RETURN", null, 0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.QUESTION_STATE_INVALID.getCode()));
    }

    private Question question(String status) {
        Question question = new Question();
        question.setId(100L);
        question.setCode("Q-001");
        question.setQuestionMode("FIXED");
        question.setQuestionType("ARITHMETIC");
        question.setStem("1 + 1 = ?");
        question.setStandardAnswer("2");
        question.setDifficulty(1);
        question.setStatus(status);
        question.setCreatedBy(7L);
        question.setVersion(0);
        return question;
    }

    private QuestionKnowledgePoint binding() {
        QuestionKnowledgePoint binding = new QuestionKnowledgePoint();
        binding.setQuestionId(100L);
        binding.setKnowledgePointId(20L);
        binding.setPrimary(true);
        binding.setWeight(BigDecimal.ONE);
        return binding;
    }

    private KnowledgePoint enabledKnowledgePoint() {
        KnowledgePoint knowledgePoint = new KnowledgePoint();
        knowledgePoint.setId(20L);
        knowledgePoint.setModuleId(10L);
        knowledgePoint.setCode("KP-001");
        knowledgePoint.setName("10 以内加法");
        knowledgePoint.setStatus(1);
        return knowledgePoint;
    }

    private ContentModule enabledModule() {
        ContentModule module = new ContentModule();
        module.setId(10L);
        module.setStatus(1);
        return module;
    }
}
