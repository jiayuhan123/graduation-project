package xyz.jiayuhan.teachingservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.teachingservice.entity.Assignment;
import xyz.jiayuhan.teachingservice.entity.AssignmentQuestion;
import xyz.jiayuhan.teachingservice.entity.AssignmentSubmission;
import xyz.jiayuhan.teachingservice.mapper.AssignmentMapper;
import xyz.jiayuhan.teachingservice.mapper.AssignmentQuestionMapper;
import xyz.jiayuhan.teachingservice.mapper.AssignmentSubmissionMapper;
import xyz.jiayuhan.teachingservice.mapper.ClassStudentMapper;
import xyz.jiayuhan.teachingservice.mapper.ClassTeacherMapper;
import xyz.jiayuhan.teachingservice.mapper.TeachingClassMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证学生只能读取发布时分配给自己的作业层级。 */
@ExtendWith(MockitoExtension.class)
class TeachingQueryServiceImplTest {

    @Mock
    private TeachingClassMapper teachingClassMapper;
    @Mock
    private ClassTeacherMapper classTeacherMapper;
    @Mock
    private ClassStudentMapper classStudentMapper;
    @Mock
    private AssignmentMapper assignmentMapper;
    @Mock
    private AssignmentQuestionMapper assignmentQuestionMapper;
    @Mock
    private AssignmentSubmissionMapper assignmentSubmissionMapper;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TeachingQueryServiceImpl queryService;

    @Test
    @SuppressWarnings("unchecked")
    void filtersLayeredQuestionsAndHidesAnswersForStudent() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
                AssignmentQuestion.class);
        Assignment assignment = new Assignment();
        assignment.setId(300L);
        assignment.setTeacherId(42L);
        assignment.setClassId(100L);
        assignment.setTitle("分层作业");
        assignment.setAssignmentType("LAYERED");
        assignment.setStatus("PUBLISHED");
        assignment.setTotalScore(BigDecimal.TEN);
        assignment.setVersion(1);
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setAssignmentId(300L);
        submission.setStudentId(501L);
        submission.setLayerCode("A");
        AssignmentQuestion question = new AssignmentQuestion();
        question.setId(301L);
        question.setAssignmentId(300L);
        question.setQuestionId(201L);
        question.setLayerCode("A");
        question.setSequenceNo(1);
        question.setScore(BigDecimal.TEN);
        question.setQuestionSnapshot("{}");
        QuestionResponse snapshot = new QuestionResponse(
                201L, "Q-A", "FIXED", "ARITHMETIC", "1 + 1 = ?", null,
                "2", "相加即可", null, 1, 20, "ENABLED", List.of(101L));
        when(assignmentMapper.selectById(300L)).thenReturn(assignment);
        when(assignmentSubmissionMapper.selectOne(any())).thenReturn(submission);
        when(assignmentQuestionMapper.selectList(any())).thenReturn(List.of(question));
        when(objectMapper.readValue("{}", QuestionResponse.class)).thenReturn(snapshot);

        var detail = queryService.getAssignmentForUser(501L, 300L);

        assertThat(detail.questions()).singleElement().satisfies(view -> {
            assertThat(view.layerCode()).isEqualTo("A");
            assertThat(view.standardAnswer()).isNull();
            assertThat(view.analysis()).isNull();
        });
        ArgumentCaptor<LambdaQueryWrapper<AssignmentQuestion>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(assignmentQuestionMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("layer_code");
        assertThat(captor.getValue().getParamNameValuePairs()).containsValue("A");
    }
}
