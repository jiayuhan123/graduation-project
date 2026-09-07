package xyz.jiayuhan.learningservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.teachingapi.model.response.StudentAccessResponse;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;

import java.util.List;
import java.util.function.Supplier;

/** 使用用户角色和真实师生关系约束跨学生学情访问。 */
@Service
@RequiredArgsConstructor
public class LearningReportServiceImpl implements LearningReportService {

    private static final String REPORT_PERMISSION = "teaching:report:query";

    private final LearningQueryService learningQueryService;
    private final UserFeignClient userFeignClient;
    private final TeachingFeignClient teachingFeignClient;

    @Override
    public LearningView.StudentReport getOwnReport(Long userId) {
        UserSummaryResponse user = requireRemoteData(
                () -> userFeignClient.getUserSummary(userId), "暂时无法读取当前用户资料");
        requireActiveStudent(user);
        return buildReport(user);
    }

    @Override
    public LearningView.StudentReport getStudentReport(Long requesterId, Long studentId) {
        UserSummaryResponse student = requireRemoteData(
                () -> userFeignClient.getUserSummary(studentId), "暂时无法读取学生资料");
        requireActiveStudent(student);
        if (!requesterId.equals(studentId)) {
            authorizeCrossStudentAccess(requesterId, studentId);
        }
        return buildReport(student);
    }

    private void authorizeCrossStudentAccess(Long requesterId, Long studentId) {
        UserSummaryResponse requester = requireRemoteData(
                () -> userFeignClient.getUserSummary(requesterId), "暂时无法读取当前用户资料");
        UserPermissionResponse permission = requireRemoteData(
                () -> userFeignClient.authorizeUser(requesterId, REPORT_PERMISSION),
                "暂时无法校验学情查询权限");
        if (!permission.exists() || !permission.active() || !permission.granted()) {
            throw new BusinessException(ResultCode.LEARNING_REPORT_FORBIDDEN);
        }
        if (requester.roles() != null && requester.roles().contains("admin")) {
            return;
        }
        StudentAccessResponse access = requireRemoteData(
                () -> teachingFeignClient.canViewStudent(requesterId, studentId),
                "暂时无法校验师生关系");
        if (!access.accessible()) {
            throw new BusinessException(ResultCode.LEARNING_REPORT_FORBIDDEN);
        }
    }

    private LearningView.StudentReport buildReport(UserSummaryResponse student) {
        Long studentId = student.id();
        List<LearningView.Mastery> weakPoints = learningQueryService.listMastery(studentId, null)
                .stream()
                .filter(item -> item.masteryScore().doubleValue() < 0.6)
                .limit(10)
                .toList();
        return new LearningView.StudentReport(
                studentId, student.username(), displayName(student), student.avatarUrl(),
                learningQueryService.getOverview(studentId),
                learningQueryService.pagePracticeTasks(studentId, 1, 10, null).records(),
                weakPoints,
                learningQueryService.pageErrors(studentId, 1, 10, null, null).records(),
                learningQueryService.pageRecommendations(studentId, 1, 10, "PENDING").records());
    }

    private String displayName(UserSummaryResponse user) {
        if (user.realName() != null && !user.realName().isBlank()) {
            return user.realName();
        }
        if (user.nickname() != null && !user.nickname().isBlank()) {
            return user.nickname();
        }
        return user.username();
    }

    private void requireActiveStudent(UserSummaryResponse user) {
        if (user.roles() == null || !user.roles().contains("student")
                || !Integer.valueOf(1).equals(user.status())) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "学生账号不存在或不可用");
        }
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
}
