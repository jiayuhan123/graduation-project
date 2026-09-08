package xyz.jiayuhan.learningservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.contentapi.model.request.BatchQuestionQueryRequest;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.learningservice.entity.PhotoMarkingItem;
import xyz.jiayuhan.learningservice.entity.PhotoMarkingTask;
import xyz.jiayuhan.learningservice.entity.PhotoReviewRecord;
import xyz.jiayuhan.learningservice.mapper.PhotoMarkingItemMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoMarkingTaskMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoReviewRecordMapper;
import xyz.jiayuhan.learningservice.model.request.ReviewPhotoItemRequest;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.teachingapi.model.response.ClassAccessResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassRosterResponse;
import xyz.jiayuhan.teachingapi.model.response.StudentAccessResponse;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.request.BatchUserQueryRequest;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** OCR 复核队列、详情、历史、重试和可信结果批量通过。 */
@Service
@RequiredArgsConstructor
public class PhotoReviewWorkbenchServiceImpl implements PhotoReviewWorkbenchService {

    private static final String REVIEW_PERMISSION = "learning:marking:review";
    private static final BigDecimal BATCH_APPROVE_THRESHOLD = new BigDecimal("0.8000");

    private final PhotoMarkingTaskMapper photoMarkingTaskMapper;
    private final PhotoMarkingItemMapper photoMarkingItemMapper;
    private final PhotoReviewRecordMapper photoReviewRecordMapper;
    private final LearningCommandService learningCommandService;
    private final UserFeignClient userFeignClient;
    private final TeachingFeignClient teachingFeignClient;
    private final ContentFeignClient contentFeignClient;

    @Override
    public PageResult<LearningView.PhotoReviewTask> pageTasks(
            Long requesterId,
            Long classId,
            Long studentId,
            long page,
            long size,
            String status,
            String reviewStatus,
            OffsetDateTime submittedFrom,
            OffsetDateTime submittedTo) {
        Reviewer reviewer = requireReviewer(requesterId);
        List<Long> scope = resolveScope(reviewer, classId, studentId);
        if (scope != null && scope.isEmpty()) {
            return new PageResult<>(page, size, 0, 0, List.of());
        }
        Page<PhotoMarkingTask> result = photoMarkingTaskMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PhotoMarkingTask>()
                        .in(scope != null, PhotoMarkingTask::getStudentId, scope)
                        .eq(studentId != null, PhotoMarkingTask::getStudentId, studentId)
                        .eq(status != null, PhotoMarkingTask::getStatus, status)
                        .eq(reviewStatus != null, PhotoMarkingTask::getReviewStatus, reviewStatus)
                        .ge(submittedFrom != null, PhotoMarkingTask::getSubmittedAt, submittedFrom)
                        .le(submittedTo != null, PhotoMarkingTask::getSubmittedAt, submittedTo)
                        .orderByAsc(PhotoMarkingTask::getStatus)
                        .orderByDesc(PhotoMarkingTask::getSubmittedAt)
                        .orderByDesc(PhotoMarkingTask::getId));
        Map<Long, UserSummaryResponse> users = usersById(result.getRecords().stream()
                .map(PhotoMarkingTask::getStudentId).distinct().toList());
        List<LearningView.PhotoReviewTask> records = result.getRecords().stream()
                .map(task -> toReviewTask(task, users.get(task.getStudentId())))
                .toList();
        return new PageResult<>(
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), records);
    }

    @Override
    public LearningView.PhotoReviewDetail getDetail(Long requesterId, Long markingTaskId) {
        Reviewer reviewer = requireReviewer(requesterId);
        PhotoMarkingTask task = requireTask(markingTaskId);
        authorizeStudent(reviewer, task.getStudentId());
        return buildDetail(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningView.PhotoReviewDetail retry(Long requesterId, Long markingTaskId) {
        Reviewer reviewer = requireReviewer(requesterId);
        PhotoMarkingTask task = requireTask(markingTaskId);
        authorizeStudent(reviewer, task.getStudentId());
        if (!"FAILED".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "只有识别失败的任务可以重试");
        }
        if (photoMarkingItemMapper.selectCount(new LambdaQueryWrapper<PhotoMarkingItem>()
                .eq(PhotoMarkingItem::getMarkingTaskId, markingTaskId)) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "任务已有识别明细，不能直接重试");
        }
        task.setStatus("UPLOADED");
        task.setFailureReason(null);
        task.setProcessedAt(null);
        task.setReviewStatus("NOT_REQUIRED");
        if (photoMarkingTaskMapper.updateById(task) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "任务状态已变化，请刷新后重试");
        }
        return buildDetail(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningView.PhotoReviewDetail reviewItem(
            Long requesterId,
            Long markingTaskId,
            Long markingItemId,
            ReviewPhotoItemRequest request) {
        Reviewer reviewer = requireReviewer(requesterId);
        PhotoMarkingTask task = requireTask(markingTaskId);
        authorizeStudent(reviewer, task.getStudentId());
        learningCommandService.reviewPhotoItem(
                requesterId, markingTaskId, markingItemId, request);
        return buildDetail(requireTask(markingTaskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningView.BatchPhotoReviewResult approveHighConfidence(
            Long requesterId, List<Long> markingTaskIds) {
        Reviewer reviewer = requireReviewer(requesterId);
        int approvedItems = 0;
        int completedTasks = 0;
        for (Long taskId : markingTaskIds.stream().distinct().toList()) {
            PhotoMarkingTask task = requireTask(taskId);
            authorizeStudent(reviewer, task.getStudentId());
            List<PhotoMarkingItem> candidates = photoMarkingItemMapper.selectList(
                    new LambdaQueryWrapper<PhotoMarkingItem>()
                            .eq(PhotoMarkingItem::getMarkingTaskId, taskId)
                            .eq(PhotoMarkingItem::getStatus, "REVIEW_REQUIRED")
                            .isNotNull(PhotoMarkingItem::getMatchedQuestionId)
                            .ge(PhotoMarkingItem::getRecognitionConfidence, BATCH_APPROVE_THRESHOLD)
                            .orderByAsc(PhotoMarkingItem::getItemNo));
            for (PhotoMarkingItem item : candidates) {
                learningCommandService.reviewPhotoItem(
                        requesterId, taskId, item.getId(),
                        new ReviewPhotoItemRequest("APPROVE", null, null, null, "高可信结果批量通过"));
                approvedItems++;
            }
            PhotoMarkingTask refreshed = requireTask(taskId);
            if (!candidates.isEmpty() && "COMPLETED".equals(refreshed.getStatus())) {
                completedTasks++;
            }
        }
        return new LearningView.BatchPhotoReviewResult(approvedItems, completedTasks);
    }

    private List<Long> resolveScope(Reviewer reviewer, Long classId, Long studentId) {
        if (classId != null) {
            if (!reviewer.admin()) {
                ClassAccessResponse access = requireRemoteData(
                        () -> teachingFeignClient.canViewClass(reviewer.userId(), classId),
                        "暂时无法校验班级管理关系");
                if (!access.accessible()) {
                    throw new BusinessException(ResultCode.LEARNING_REPORT_FORBIDDEN);
                }
            }
            ClassRosterResponse roster = requireRemoteData(
                    () -> teachingFeignClient.getClassRoster(classId), "暂时无法读取班级花名册");
            List<Long> ids = roster.studentIds() == null ? List.of() : roster.studentIds();
            if (studentId != null && !ids.contains(studentId)) {
                throw new BusinessException(ResultCode.LEARNING_REPORT_FORBIDDEN);
            }
            return ids;
        }
        if (studentId != null) {
            authorizeStudent(reviewer, studentId);
            return List.of(studentId);
        }
        if (!reviewer.admin()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "教师请先选择一个班级");
        }
        return null;
    }

    private Reviewer requireReviewer(Long requesterId) {
        UserSummaryResponse user = requireRemoteData(
                () -> userFeignClient.getUserSummary(requesterId), "暂时无法读取当前用户资料");
        UserPermissionResponse permission = requireRemoteData(
                () -> userFeignClient.authorizeUser(requesterId, REVIEW_PERMISSION),
                "暂时无法校验拍照复核权限");
        if (!permission.exists() || !permission.active() || !permission.granted()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return new Reviewer(requesterId, user.roles() != null && user.roles().contains("admin"));
    }

    private void authorizeStudent(Reviewer reviewer, Long studentId) {
        if (reviewer.admin()) {
            return;
        }
        StudentAccessResponse access = requireRemoteData(
                () -> teachingFeignClient.canViewStudent(reviewer.userId(), studentId),
                "暂时无法校验师生关系");
        if (!access.accessible()) {
            throw new BusinessException(ResultCode.LEARNING_REPORT_FORBIDDEN);
        }
    }

    private LearningView.PhotoReviewDetail buildDetail(PhotoMarkingTask task) {
        List<PhotoMarkingItem> items = photoMarkingItemMapper.selectList(
                new LambdaQueryWrapper<PhotoMarkingItem>()
                        .eq(PhotoMarkingItem::getMarkingTaskId, task.getId())
                        .orderByAsc(PhotoMarkingItem::getItemNo));
        Map<Long, QuestionResponse> questions = questionsById(items.stream()
                .map(PhotoMarkingItem::getMatchedQuestionId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList());
        List<PhotoReviewRecord> history = photoReviewRecordMapper.selectList(
                new LambdaQueryWrapper<PhotoReviewRecord>()
                        .eq(PhotoReviewRecord::getMarkingTaskId, task.getId())
                        .orderByDesc(PhotoReviewRecord::getReviewedAt)
                        .orderByDesc(PhotoReviewRecord::getId));
        List<Long> userIds = new ArrayList<>();
        userIds.add(task.getStudentId());
        history.stream().map(PhotoReviewRecord::getReviewerId).forEach(userIds::add);
        Map<Long, UserSummaryResponse> users = usersById(userIds.stream().distinct().toList());
        LearningView.PhotoReviewTask summary = toReviewTask(task, users.get(task.getStudentId()));
        List<LearningView.PhotoReviewItem> itemViews = items.stream().map(item -> {
            QuestionResponse question = questions.get(item.getMatchedQuestionId());
            LearningView.QuestionReference reference = question == null ? null
                    : new LearningView.QuestionReference(
                            question.id(), question.code(), question.stem(), question.standardAnswer());
            return new LearningView.PhotoReviewItem(toView(item), reference);
        }).toList();
        List<LearningView.PhotoReviewHistory> historyViews = history.stream()
                .map(record -> new LearningView.PhotoReviewHistory(
                        record.getId(), record.getMarkingItemId(), record.getReviewerId(),
                        displayName(users.get(record.getReviewerId()), record.getReviewerId()),
                        record.getDecision(), record.getBeforeData(), record.getAfterData(),
                        record.getComment(), record.getReviewedAt()))
                .toList();
        return new LearningView.PhotoReviewDetail(summary, itemViews, historyViews);
    }

    private LearningView.PhotoReviewTask toReviewTask(
            PhotoMarkingTask task, UserSummaryResponse student) {
        long pendingItems = photoMarkingItemMapper.selectCount(
                new LambdaQueryWrapper<PhotoMarkingItem>()
                        .eq(PhotoMarkingItem::getMarkingTaskId, task.getId())
                        .eq(PhotoMarkingItem::getStatus, "REVIEW_REQUIRED"));
        return new LearningView.PhotoReviewTask(
                toView(task), task.getStudentId(), displayName(student, task.getStudentId()),
                pendingItems);
    }

    private LearningView.PhotoTask toView(PhotoMarkingTask task) {
        return new LearningView.PhotoTask(
                task.getId(), task.getOriginalFileId(), task.getPracticeTaskId(), task.getStatus(),
                task.getPageCount(), task.getOverallConfidence(), task.getRecognizedItemCount(),
                task.getReviewStatus(), task.getSubmittedAt(), task.getProcessedAt(),
                task.getReviewedAt(), task.getFailureReason());
    }

    private LearningView.PhotoItem toView(PhotoMarkingItem item) {
        return new LearningView.PhotoItem(
                item.getId(), item.getItemNo(), item.getCropFileId(), item.getBoundingBox(),
                item.getRecognizedStem(), item.getRecognizedAnswer(), item.getMatchedQuestionId(),
                item.getRecognitionConfidence(), item.getTaskQuestionId(), item.getAnswerRecordId(),
                item.getStatus());
    }

    private PhotoMarkingTask requireTask(Long markingTaskId) {
        PhotoMarkingTask task = photoMarkingTaskMapper.selectById(markingTaskId);
        if (task == null) {
            throw new BusinessException(ResultCode.PHOTO_MARKING_TASK_NOT_FOUND);
        }
        return task;
    }

    private Map<Long, UserSummaryResponse> usersById(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserSummaryResponse> users = new HashMap<>();
        for (int start = 0; start < userIds.size(); start += 100) {
            List<Long> part = userIds.subList(start, Math.min(start + 100, userIds.size()));
            List<UserSummaryResponse> result = requireRemoteData(
                    () -> userFeignClient.getUserSummaries(new BatchUserQueryRequest(part)),
                    "暂时无法读取用户资料");
            result.forEach(user -> users.put(user.id(), user));
        }
        return users;
    }

    private Map<Long, QuestionResponse> questionsById(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        try {
            Result<List<QuestionResponse>> result = contentFeignClient.getQuestions(
                    new BatchQuestionQueryRequest(questionIds));
            if (result == null || !Integer.valueOf(200).equals(result.getCode())
                    || result.getData() == null) {
                return Map.of();
            }
            return result.getData().stream()
                    .collect(Collectors.toMap(QuestionResponse::id, Function.identity()));
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }

    private String displayName(UserSummaryResponse user, Long userId) {
        if (user == null) {
            return "用户 #" + userId;
        }
        if (user.realName() != null && !user.realName().isBlank()) {
            return user.realName();
        }
        if (user.nickname() != null && !user.nickname().isBlank()) {
            return user.nickname();
        }
        return user.username();
    }

    private <T> T requireRemoteData(Supplier<Result<T>> request, String message) {
        Result<T> result;
        try {
            result = request.get();
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, message);
        }
        if (result == null || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, message);
        }
        return result.getData();
    }

    private record Reviewer(Long userId, boolean admin) {
    }
}
