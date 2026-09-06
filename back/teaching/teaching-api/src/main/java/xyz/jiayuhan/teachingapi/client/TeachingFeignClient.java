package xyz.jiayuhan.teachingapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.teachingapi.TeachingApiConstants;
import xyz.jiayuhan.teachingapi.model.response.AssignmentSummaryResponse;

/** 教学服务内部只读客户端。 */
@FeignClient(
        name = TeachingApiConstants.SERVICE_NAME,
        contextId = "teachingFeignClient",
        path = TeachingApiConstants.INTERNAL_PATH,
        configuration = TeachingFeignConfiguration.class
)
public interface TeachingFeignClient {

    @GetMapping("/assignments/{assignmentId}")
    Result<AssignmentSummaryResponse> getAssignment(@PathVariable("assignmentId") Long assignmentId);
}
