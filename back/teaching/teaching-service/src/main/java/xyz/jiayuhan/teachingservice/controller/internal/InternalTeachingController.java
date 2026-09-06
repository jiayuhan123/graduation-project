package xyz.jiayuhan.teachingservice.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.teachingapi.TeachingApiConstants;
import xyz.jiayuhan.teachingapi.model.response.AssignmentSummaryResponse;
import xyz.jiayuhan.teachingservice.service.TeachingQueryService;

/** 仅供学习服务调用的作业查询契约。 */
@RestController
@RequestMapping(TeachingApiConstants.INTERNAL_PATH)
@RequiredArgsConstructor
public class InternalTeachingController {

    private final TeachingQueryService teachingQueryService;

    @GetMapping("/assignments/{assignmentId}")
    public Result<AssignmentSummaryResponse> getAssignment(@PathVariable Long assignmentId) {
        return Result.success(teachingQueryService.getAssignmentInternal(assignmentId));
    }
}
