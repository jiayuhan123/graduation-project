package xyz.jiayuhan.learningservice.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.learningapi.LearningApiConstants;
import xyz.jiayuhan.learningapi.model.response.PracticeTaskSummaryResponse;
import xyz.jiayuhan.learningservice.service.LearningQueryService;

/** 仅供教学与游戏服务调用的练习任务查询接口。 */
@RestController
@RequestMapping(LearningApiConstants.INTERNAL_PATH)
@RequiredArgsConstructor
public class InternalLearningController {

    private final LearningQueryService learningQueryService;

    @GetMapping("/practice-tasks/{taskId}")
    public Result<PracticeTaskSummaryResponse> getPracticeTask(@PathVariable Long taskId) {
        return Result.success(learningQueryService.getPracticeTaskInternal(taskId));
    }
}
