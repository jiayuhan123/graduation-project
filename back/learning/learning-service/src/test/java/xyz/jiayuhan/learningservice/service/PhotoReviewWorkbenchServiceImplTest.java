package xyz.jiayuhan.learningservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.learningservice.entity.PhotoMarkingTask;
import xyz.jiayuhan.learningservice.mapper.PhotoMarkingItemMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoMarkingTaskMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoReviewRecordMapper;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoReviewWorkbenchServiceImplTest {

    @Mock private PhotoMarkingTaskMapper photoMarkingTaskMapper;
    @Mock private PhotoMarkingItemMapper photoMarkingItemMapper;
    @Mock private PhotoReviewRecordMapper photoReviewRecordMapper;
    @Mock private LearningCommandService learningCommandService;
    @Mock private UserFeignClient userFeignClient;
    @Mock private TeachingFeignClient teachingFeignClient;
    @Mock private ContentFeignClient contentFeignClient;
    @InjectMocks private PhotoReviewWorkbenchServiceImpl service;

    @Test
    void adminCanRetryFailedTaskWithoutRecognizedItems() {
        long adminId = 1L;
        PhotoMarkingTask task = new PhotoMarkingTask();
        task.setId(5L);
        task.setStudentId(9L);
        task.setOriginalFileId(java.util.UUID.randomUUID());
        task.setStatus("FAILED");
        task.setReviewStatus("NOT_REQUIRED");
        task.setFailureReason("OCR timeout");
        task.setPageCount(1);
        task.setRecognizedItemCount(0);
        when(userFeignClient.getUserSummary(adminId)).thenReturn(Result.success(
                new UserSummaryResponse(adminId, "admin", "管理员", null, null, 1,
                        List.of("admin"), null, null)));
        when(userFeignClient.authorizeUser(adminId, "learning:marking:review"))
                .thenReturn(Result.success(new UserPermissionResponse(
                        adminId, "learning:marking:review", true, true, true)));
        when(photoMarkingTaskMapper.selectById(5L)).thenReturn(task);
        when(photoMarkingItemMapper.selectCount(any())).thenReturn(0L);
        when(photoMarkingItemMapper.selectList(any())).thenReturn(List.of());
        when(photoReviewRecordMapper.selectList(any())).thenReturn(List.of());
        when(userFeignClient.getUserSummaries(any())).thenReturn(Result.success(List.of(
                new UserSummaryResponse(9L, "student", "学生", null, null, 1,
                        List.of("student"), 4, 1))));
        when(photoMarkingTaskMapper.updateById(task)).thenReturn(1);

        var result = service.retry(adminId, 5L);

        assertThat(result.summary().task().status()).isEqualTo("UPLOADED");
        assertThat(result.summary().task().failureReason()).isNull();
        assertThat(result.items()).isEmpty();
    }
}
