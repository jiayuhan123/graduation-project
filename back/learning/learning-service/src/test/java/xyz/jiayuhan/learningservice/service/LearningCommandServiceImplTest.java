package xyz.jiayuhan.learningservice.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.contentapi.model.response.KnowledgePointResponse;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.learningservice.entity.AnswerRecord;
import xyz.jiayuhan.learningservice.entity.ErrorRecord;
import xyz.jiayuhan.learningservice.entity.MasteryChangeLog;
import xyz.jiayuhan.learningservice.entity.MasteryRecord;
import xyz.jiayuhan.learningservice.entity.PhotoMarkingItem;
import xyz.jiayuhan.learningservice.entity.PhotoMarkingTask;
import xyz.jiayuhan.learningservice.entity.PracticeTask;
import xyz.jiayuhan.learningservice.entity.PracticeTaskKnowledgePoint;
import xyz.jiayuhan.learningservice.entity.PracticeTaskQuestion;
import xyz.jiayuhan.learningservice.entity.Recommendation;
import xyz.jiayuhan.learningservice.event.AssignmentProgressOutboxService;
import xyz.jiayuhan.learningservice.mapper.AnswerRecordMapper;
import xyz.jiayuhan.learningservice.mapper.ErrorRecordMapper;
import xyz.jiayuhan.learningservice.mapper.MasteryChangeLogMapper;
import xyz.jiayuhan.learningservice.mapper.MasteryRecordMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoMarkingItemMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoMarkingTaskMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoReviewRecordMapper;
import xyz.jiayuhan.learningservice.mapper.PracticeTaskKnowledgePointMapper;
import xyz.jiayuhan.learningservice.mapper.PracticeTaskMapper;
import xyz.jiayuhan.learningservice.mapper.PracticeTaskQuestionMapper;
import xyz.jiayuhan.learningservice.mapper.RecommendationMapper;
import xyz.jiayuhan.learningservice.model.request.CreatePracticeTaskRequest;
import xyz.jiayuhan.learningservice.model.request.CreatePhotoMarkingRequest;
import xyz.jiayuhan.learningservice.model.request.PhotoRecognitionItemRequest;
import xyz.jiayuhan.learningservice.model.request.PhotoRecognitionRequest;
import xyz.jiayuhan.learningservice.model.request.SubmitAnswerRequest;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.learningservice.support.AnswerGrader;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LearningCommandServiceImplTest {

    private static final Long STUDENT_ID = 101L;
    private static final Long KNOWLEDGE_POINT_ID = 501L;

    @Mock private PracticeTaskMapper practiceTaskMapper;
    @Mock private PracticeTaskKnowledgePointMapper taskKnowledgePointMapper;
    @Mock private PracticeTaskQuestionMapper taskQuestionMapper;
    @Mock private AnswerRecordMapper answerRecordMapper;
    @Mock private ErrorRecordMapper errorRecordMapper;
    @Mock private MasteryRecordMapper masteryRecordMapper;
    @Mock private MasteryChangeLogMapper masteryChangeLogMapper;
    @Mock private RecommendationMapper recommendationMapper;
    @Mock private PhotoMarkingTaskMapper photoMarkingTaskMapper;
    @Mock private PhotoMarkingItemMapper photoMarkingItemMapper;
    @Mock private PhotoReviewRecordMapper photoReviewRecordMapper;
    @Mock private ContentFeignClient contentFeignClient;
    @Mock private TeachingFeignClient teachingFeignClient;
    @Mock private UserFeignClient userFeignClient;
    @Mock private LearningQueryService learningQueryService;
    @Mock private AssignmentProgressOutboxService assignmentProgressOutboxService;

    private ObjectMapper objectMapper;
    private LearningCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, ErrorRecord.class);
        TableInfoHelper.initTableInfo(assistant, Recommendation.class);
        TableInfoHelper.initTableInfo(assistant, PhotoMarkingTask.class);
        objectMapper = JsonMapper.builder().build();
        service = new LearningCommandServiceImpl(
                practiceTaskMapper,
                taskKnowledgePointMapper,
                taskQuestionMapper,
                answerRecordMapper,
                errorRecordMapper,
                masteryRecordMapper,
                masteryChangeLogMapper,
                recommendationMapper,
                photoMarkingTaskMapper,
                photoMarkingItemMapper,
                photoReviewRecordMapper,
                contentFeignClient,
                teachingFeignClient,
                userFeignClient,
                learningQueryService,
                assignmentProgressOutboxService,
                objectMapper,
                new AnswerGrader(objectMapper));
        lenient().when(practiceTaskMapper.updateById(any(PracticeTask.class))).thenReturn(1);
        lenient().when(answerRecordMapper.updateById(any(AnswerRecord.class))).thenReturn(1);
        lenient().when(masteryRecordMapper.updateById(any(MasteryRecord.class))).thenReturn(1);
        lenient().when(masteryRecordMapper.acquireUpdateLock(anyLong(), anyLong())).thenReturn(1);
        lenient().when(photoMarkingTaskMapper.updateById(any(PhotoMarkingTask.class))).thenReturn(1);
        lenient().when(photoMarkingItemMapper.updateById(any(PhotoMarkingItem.class))).thenReturn(1);
    }

    @Test
    void createsSelfPracticeFromEnabledFixedQuestionPool() {
        QuestionResponse question = question(701L, "{\"value\":42}");
        when(userFeignClient.validateUser(STUDENT_ID, "student"))
                .thenReturn(Result.success(new UserValidationResponse(STUDENT_ID, true, true, true)));
        when(contentFeignClient.getKnowledgePoint(KNOWLEDGE_POINT_ID))
                .thenReturn(Result.success(new KnowledgePointResponse(
                        KNOWLEDGE_POINT_ID, 1L, null, "KP", "整数计算", null, 2, 1, 1)));
        when(contentFeignClient.pageQuestions(1, 100, null, KNOWLEDGE_POINT_ID, "FIXED", null, 3))
                .thenReturn(Result.success(new PageResult<>(1, 100, 1, 1, List.of(question))));
        doAnswer(invocation -> {
            PracticeTask task = invocation.getArgument(0);
            task.setId(801L);
            return 1;
        }).when(practiceTaskMapper).insert(any(PracticeTask.class));
        doAnswer(invocation -> {
            PracticeTaskQuestion taskQuestion = invocation.getArgument(0);
            taskQuestion.setId(901L);
            return 1;
        }).when(taskQuestionMapper).insert(any(PracticeTaskQuestion.class));
        LearningView.PracticeDetail detail = detail(801L, "SELF", "PENDING");
        when(learningQueryService.getPracticeTask(STUDENT_ID, 801L)).thenReturn(detail);

        LearningView.PracticeDetail result = service.createSelfPractice(
                STUDENT_ID,
                new CreatePracticeTaskRequest(
                        KNOWLEDGE_POINT_ID, 3, 1, "NORMAL", null, new BigDecimal("0.8000")));

        assertThat(result).isSameAs(detail);
        verify(practiceTaskMapper).insert(any(PracticeTask.class));
        verify(taskQuestionMapper).insert(any(PracticeTaskQuestion.class));
        verify(taskKnowledgePointMapper).insert(any(PracticeTaskKnowledgePoint.class));
    }

    @Test
    void gradesSelfPracticeImmediatelyAndAllowsRetryWhileUpdatingSignals() {
        PracticeTask task = task(810L, "SELF", "IN_PROGRESS");
        PracticeTaskQuestion taskQuestion = taskQuestion(910L, task.getId(), question(710L, "{\"value\":8}"));
        when(practiceTaskMapper.selectForUpdate(task.getId(), STUDENT_ID)).thenReturn(task);
        when(taskQuestionMapper.selectOne(any())).thenReturn(taskQuestion);
        when(answerRecordMapper.selectCount(any())).thenReturn(0L, 1L);
        AtomicInteger answerIds = new AtomicInteger(1000);
        doAnswer(invocation -> {
            AnswerRecord answer = invocation.getArgument(0);
            answer.setId((long) answerIds.incrementAndGet());
            return 1;
        }).when(answerRecordMapper).insert(any(AnswerRecord.class));
        AtomicReference<MasteryRecord> mastery = new AtomicReference<>();
        when(masteryRecordMapper.selectOne(any())).thenAnswer(invocation -> mastery.get());
        doAnswer(invocation -> {
            MasteryRecord record = invocation.getArgument(0);
            record.setId(1101L);
            mastery.set(record);
            return 1;
        }).when(masteryRecordMapper).insert(any(MasteryRecord.class));

        LearningView.AnswerFeedback first = service.submitAnswer(
                STUDENT_ID, task.getId(), taskQuestion.getId(), new SubmitAnswerRequest("7", 1200));
        LearningView.AnswerFeedback retry = service.submitAnswer(
                STUDENT_ID, task.getId(), taskQuestion.getId(), new SubmitAnswerRequest("8", 900));

        assertThat(first.solutionVisible()).isTrue();
        assertThat(first.answer().correct()).isFalse();
        assertThat(first.answer().attemptNo()).isEqualTo(1);
        assertThat(retry.answer().correct()).isTrue();
        assertThat(retry.answer().attemptNo()).isEqualTo(2);
        assertThat(mastery.get().getTotalAnswerCount()).isEqualTo(2);
        assertThat(mastery.get().getCorrectAnswerCount()).isEqualTo(1);
        verify(errorRecordMapper).insert(any(ErrorRecord.class));
        verify(recommendationMapper).insert(any(Recommendation.class));
        verify(masteryChangeLogMapper, org.mockito.Mockito.times(2))
                .insert(any(MasteryChangeLog.class));
    }

    @Test
    void keepsAssignmentAnswerPendingUntilFinalSubmissionThenGradesOnce() {
        PracticeTask task = task(820L, "ASSIGNMENT", "IN_PROGRESS");
        task.setSourceId("77");
        PracticeTaskQuestion taskQuestion = taskQuestion(920L, task.getId(), question(720L, "{\"value\":12}"));
        when(practiceTaskMapper.selectForUpdate(task.getId(), STUDENT_ID)).thenReturn(task);
        when(taskQuestionMapper.selectOne(any())).thenReturn(taskQuestion);
        when(taskQuestionMapper.selectList(any())).thenReturn(List.of(taskQuestion));
        AtomicReference<AnswerRecord> savedAnswer = new AtomicReference<>();
        when(answerRecordMapper.selectOne(any())).thenAnswer(invocation -> savedAnswer.get());
        doAnswer(invocation -> {
            AnswerRecord answer = invocation.getArgument(0);
            answer.setId(1020L);
            savedAnswer.set(answer);
            return 1;
        }).when(answerRecordMapper).insert(any(AnswerRecord.class));
        doAnswer(invocation -> {
            MasteryRecord mastery = invocation.getArgument(0);
            mastery.setId(1120L);
            return 1;
        }).when(masteryRecordMapper).insert(any(MasteryRecord.class));
        LearningView.PracticeDetail completed = detail(task.getId(), "ASSIGNMENT", "COMPLETED");
        when(learningQueryService.getPracticeTask(STUDENT_ID, task.getId())).thenReturn(completed);

        LearningView.AnswerFeedback saved = service.submitAnswer(
                STUDENT_ID, task.getId(), taskQuestion.getId(), new SubmitAnswerRequest("11", 1500));

        assertThat(saved.solutionVisible()).isFalse();
        assertThat(saved.answer().gradingStatus()).isEqualTo("PENDING");
        assertThat(saved.answer().correct()).isNull();
        verify(masteryRecordMapper, never()).acquireUpdateLock(anyLong(), anyLong());

        LearningView.PracticeDetail result = service.completePractice(STUDENT_ID, task.getId());

        assertThat(result).isSameAs(completed);
        assertThat(savedAnswer.get().getGradingStatus()).isEqualTo("GRADED");
        assertThat(savedAnswer.get().getCorrect()).isFalse();
        verify(errorRecordMapper).insert(any(ErrorRecord.class));
        verify(recommendationMapper).insert(any(Recommendation.class));
        verify(assignmentProgressOutboxService).append(
                any(PracticeTask.class),
                org.mockito.ArgumentMatchers.eq("GRADED"),
                org.mockito.ArgumentMatchers.eq(BigDecimal.ZERO),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(1),
                any(OffsetDateTime.class));
    }

    @Test
    void createsIdempotentPhotoMarkingTaskForUploadedFile() {
        UUID fileId = UUID.randomUUID();
        LearningView.PhotoDetail expected = photoDetail(1301L, fileId, "UPLOADED");
        when(userFeignClient.validateUser(STUDENT_ID, "student"))
                .thenReturn(Result.success(new UserValidationResponse(STUDENT_ID, true, true, true)));
        when(photoMarkingTaskMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            PhotoMarkingTask task = invocation.getArgument(0);
            task.setId(1301L);
            return 1;
        }).when(photoMarkingTaskMapper).insertIfAbsent(any(PhotoMarkingTask.class));
        when(learningQueryService.getPhotoTask(STUDENT_ID, 1301L)).thenReturn(expected);

        LearningView.PhotoDetail result = service.createPhotoMarking(
                STUDENT_ID, new CreatePhotoMarkingRequest(fileId, 1));

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<PhotoMarkingTask> taskCaptor = ArgumentCaptor.forClass(PhotoMarkingTask.class);
        verify(photoMarkingTaskMapper).insertIfAbsent(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo("UPLOADED");
        assertThat(taskCaptor.getValue().getReviewStatus()).isEqualTo("NOT_REQUIRED");
    }

    @Test
    void routesLowConfidenceRecognitionToManualReviewWithoutGrading() {
        UUID fileId = UUID.randomUUID();
        PhotoMarkingTask markingTask = new PhotoMarkingTask();
        markingTask.setId(1401L);
        markingTask.setStudentId(STUDENT_ID);
        markingTask.setOriginalFileId(fileId);
        markingTask.setStatus("UPLOADED");
        markingTask.setPageCount(1);
        markingTask.setVersion(0);
        when(photoMarkingTaskMapper.selectForUpdate(1401L)).thenReturn(markingTask);
        when(photoMarkingItemMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            PracticeTask task = invocation.getArgument(0);
            task.setId(1501L);
            return 1;
        }).when(practiceTaskMapper).insert(any(PracticeTask.class));
        QuestionResponse question = question(701L, "{\"value\":42}");
        when(contentFeignClient.getQuestion(701L)).thenReturn(Result.success(question));
        LearningView.PhotoDetail expected = photoDetail(1401L, fileId, "REVIEW_REQUIRED");
        when(learningQueryService.getPhotoTask(STUDENT_ID, 1401L)).thenReturn(expected);

        LearningView.PhotoDetail result = service.applyPhotoRecognition(
                1401L,
                new PhotoRecognitionRequest(List.of(new PhotoRecognitionItemRequest(
                        1, null, Map.of("x", 10, "y", 20), "计算", "41", 701L,
                        new BigDecimal("0.7200")))));

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<PhotoMarkingItem> itemCaptor = ArgumentCaptor.forClass(PhotoMarkingItem.class);
        verify(photoMarkingItemMapper).insert(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getStatus()).isEqualTo("REVIEW_REQUIRED");
        assertThat(markingTask.getReviewStatus()).isEqualTo("PENDING");
        verify(answerRecordMapper, never()).insert(any(AnswerRecord.class));
    }

    private PracticeTask task(Long id, String sourceType, String status) {
        PracticeTask task = new PracticeTask();
        task.setId(id);
        task.setStudentId(STUDENT_ID);
        task.setSourceType(sourceType);
        task.setTitle("练习");
        task.setMode("NORMAL");
        task.setStatus(status);
        task.setQuestionCount(1);
        task.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        task.setVersion(0);
        return task;
    }

    private PracticeTaskQuestion taskQuestion(Long id, Long taskId, QuestionResponse question) {
        PracticeTaskQuestion taskQuestion = new PracticeTaskQuestion();
        taskQuestion.setId(id);
        taskQuestion.setTaskId(taskId);
        taskQuestion.setQuestionId(question.id());
        taskQuestion.setSequenceNo(1);
        taskQuestion.setQuestionSnapshot(objectMapper.writeValueAsString(question));
        taskQuestion.setMaxScore(BigDecimal.ONE);
        return taskQuestion;
    }

    private QuestionResponse question(Long id, String standardAnswer) {
        return new QuestionResponse(
                id, "Q-" + id, "FIXED", "ARITHMETIC", "计算", null,
                standardAnswer, "解析", null, 3, 30, "ENABLED", List.of(KNOWLEDGE_POINT_ID));
    }

    private LearningView.PracticeDetail detail(Long taskId, String sourceType, String status) {
        return new LearningView.PracticeDetail(
                new xyz.jiayuhan.learningapi.model.response.PracticeTaskSummaryResponse(
                        taskId, STUDENT_ID, sourceType, null, "练习", "NORMAL", status,
                        1, null, null, null, null),
                List.of(KNOWLEDGE_POINT_ID),
                List.of());
    }

    private LearningView.PhotoDetail photoDetail(Long taskId, UUID fileId, String status) {
        return new LearningView.PhotoDetail(
                new LearningView.PhotoTask(
                        taskId, fileId, null, status, 1, null, 0,
                        "UPLOADED".equals(status) ? "NOT_REQUIRED" : "PENDING",
                        OffsetDateTime.now(ZoneOffset.UTC), null, null, null),
                List.of());
    }
}
