package xyz.jiayuhan.learningapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.learningapi.LearningApiConstants;
import xyz.jiayuhan.learningapi.model.response.PracticeTaskSummaryResponse;

/** 学习闭环内部只读客户端。 */
@FeignClient(
        name = LearningApiConstants.SERVICE_NAME,
        contextId = "learningFeignClient",
        path = LearningApiConstants.INTERNAL_PATH,
        configuration = LearningFeignConfiguration.class
)
public interface LearningFeignClient {

    @GetMapping("/practice-tasks/{taskId}")
    Result<PracticeTaskSummaryResponse> getPracticeTask(@PathVariable("taskId") Long taskId);
}
