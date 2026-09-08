package xyz.jiayuhan.learningservice.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.RequestParam;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.learningapi.LearningApiConstants;
import xyz.jiayuhan.learningapi.model.response.PracticeTaskSummaryResponse;
import xyz.jiayuhan.learningservice.service.LearningQueryService;
import xyz.jiayuhan.learningservice.service.LearningCommandService;
import xyz.jiayuhan.learningservice.model.request.PhotoRecognitionRequest;
import xyz.jiayuhan.learningservice.model.request.PhotoRecognitionFailureRequest;
import xyz.jiayuhan.learningservice.model.response.LearningView;

import java.util.List;

/** 仅供教学与游戏服务调用的练习任务查询接口。 */
@RestController
@RequestMapping(LearningApiConstants.INTERNAL_PATH)
@RequiredArgsConstructor
@Validated
public class InternalLearningController {

    private final LearningQueryService learningQueryService;
    private final LearningCommandService learningCommandService;

    @GetMapping("/practice-tasks/{taskId}")
    public Result<PracticeTaskSummaryResponse> getPracticeTask(@PathVariable Long taskId) {
        return Result.success(learningQueryService.getPracticeTaskInternal(taskId));
    }

    /** OCR 工作器轮询待处理任务，文件内容通过文件服务内部接口读取。 */
    @GetMapping("/photo-marking/pending")
    public Result<List<LearningView.PhotoWorkItem>> listPendingPhotoTasks(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return Result.success(learningQueryService.listPendingPhotoTasks(limit));
    }

    /** OCR 工作器通过内部令牌回传结构化切题结果。 */
    @PostMapping("/photo-marking/{markingTaskId}/recognition")
    public Result<LearningView.PhotoDetail> applyPhotoRecognition(
            @PathVariable Long markingTaskId,
            @Valid @RequestBody PhotoRecognitionRequest request) {
        return Result.success(learningCommandService.applyPhotoRecognition(markingTaskId, request));
    }

    /** OCR 工作器领取任务；并发领取时仅第一个调用成功。 */
    @PostMapping("/photo-marking/{markingTaskId}/start")
    public Result<LearningView.PhotoDetail> startPhotoRecognition(
            @PathVariable Long markingTaskId) {
        return Result.success(learningCommandService.startPhotoRecognition(markingTaskId));
    }

    /** OCR 工作器回传识别失败，供学生和运维查看明确状态。 */
    @PostMapping("/photo-marking/{markingTaskId}/failure")
    public Result<LearningView.PhotoDetail> failPhotoRecognition(
            @PathVariable Long markingTaskId,
            @Valid @RequestBody PhotoRecognitionFailureRequest request) {
        return Result.success(learningCommandService.failPhotoRecognition(markingTaskId, request));
    }
}
