package xyz.jiayuhan.teachingservice.controller.teacher;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.teachingapi.model.response.AssignmentSummaryResponse;
import xyz.jiayuhan.teachingservice.model.request.ChangeAssignmentStatusRequest;
import xyz.jiayuhan.teachingservice.model.request.CreateAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.PublishAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateSubmissionCommentRequest;
import xyz.jiayuhan.teachingservice.model.response.TeachingView;
import xyz.jiayuhan.teachingservice.service.TeachingCommandService;
import xyz.jiayuhan.teachingservice.service.TeachingQueryService;

import java.util.List;

/** 教师作业、选题、发布与提交查看接口。 */
@Tag(name = "教师作业管理")
@Validated
@RestController
@RequestMapping("/api/teaching/teacher")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeachingQueryService queryService;
    private final TeachingCommandService commandService;

    @GetMapping("/assignments")
    public Result<PageResult<AssignmentSummaryResponse>> pageAssignments(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false)
            @Pattern(regexp = "DRAFT|PUBLISHED|CLOSED|CANCELLED", message = "作业状态不正确")
            String status) {
        return Result.success(queryService.pageCreatedAssignments(
                UserContext.requireUserId(), page, size, status));
    }

    @GetMapping("/assignments/{assignmentId}")
    public Result<TeachingView.AssignmentDetail> getAssignment(@PathVariable Long assignmentId) {
        return Result.success(queryService.getAssignmentForUser(
                UserContext.requireUserId(), assignmentId));
    }

    @PostMapping("/assignments")
    public Result<TeachingView.AssignmentDetail> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request) {
        return Result.success("作业草稿创建成功",
                commandService.createAssignment(UserContext.requireUserId(), request));
    }

    @PutMapping("/assignments/{assignmentId}")
    public Result<TeachingView.AssignmentDetail> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody UpdateAssignmentRequest request) {
        return Result.success("作业草稿修改成功",
                commandService.updateAssignment(UserContext.requireUserId(), assignmentId, request));
    }

    @PostMapping("/assignments/{assignmentId}/publish")
    public Result<TeachingView.AssignmentDetail> publishAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody PublishAssignmentRequest request) {
        return Result.success("作业发布成功",
                commandService.publishAssignment(UserContext.requireUserId(), assignmentId, request));
    }

    @PostMapping("/assignments/{assignmentId}/status")
    public Result<TeachingView.AssignmentDetail> changeStatus(
            @PathVariable Long assignmentId,
            @Valid @RequestBody ChangeAssignmentStatusRequest request) {
        return Result.success(commandService.changeAssignmentStatus(
                UserContext.requireUserId(), assignmentId, request));
    }

    @GetMapping("/question-candidates")
    public Result<PageResult<TeachingView.QuestionCandidate>> pageQuestionCandidates(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long knowledgePointId,
            @RequestParam(required = false)
            @Pattern(regexp = "ARITHMETIC|FILL_BLANK|VERTICAL|CHOICE|JUDGEMENT", message = "题型不正确")
            String questionType,
            @RequestParam(required = false) @Min(1) @Max(5) Integer difficulty) {
        return Result.success(commandService.pageQuestionCandidates(
                page, size, keyword, knowledgePointId, questionType, difficulty));
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public Result<List<TeachingView.SubmissionDetail>> listSubmissions(
            @PathVariable Long assignmentId,
            @RequestParam(required = false)
            @Pattern(regexp = "ASSIGNED|IN_PROGRESS|SUBMITTED|GRADED|OVERDUE", message = "提交状态不正确")
            String status) {
        return Result.success(commandService.listAssignmentSubmissions(
                UserContext.requireUserId(), assignmentId, status));
    }

    @PutMapping("/submissions/{submissionId}/comment")
    public Result<TeachingView.SubmissionDetail> updateSubmissionComment(
            @PathVariable Long submissionId,
            @Valid @RequestBody UpdateSubmissionCommentRequest request) {
        return Result.success("教师评语已保存",
                commandService.updateSubmissionComment(
                        UserContext.requireUserId(), submissionId, request));
    }
}
