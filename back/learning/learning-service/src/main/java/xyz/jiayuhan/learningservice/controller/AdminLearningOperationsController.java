package xyz.jiayuhan.learningservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.learningservice.model.response.LearningOperationsStatisticsResponse;
import xyz.jiayuhan.learningservice.service.LearningOperationsService;

/** 管理员全局学习和 OCR 运营统计。 */
@RestController
@RequestMapping("/api/learning/admin")
@RequiredArgsConstructor
public class AdminLearningOperationsController {

    private final LearningOperationsService learningOperationsService;

    @GetMapping("/statistics")
    public Result<LearningOperationsStatisticsResponse> statistics() {
        return Result.success(learningOperationsService.statistics(UserContext.requireUserId()));
    }
}
