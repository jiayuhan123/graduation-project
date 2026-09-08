package xyz.jiayuhan.contentservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.data.model.VersionedEntity;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.contentservice.entity.ContentModule;
import xyz.jiayuhan.contentservice.entity.ErrorType;
import xyz.jiayuhan.contentservice.entity.KnowledgePoint;
import xyz.jiayuhan.contentservice.entity.Question;
import xyz.jiayuhan.contentservice.entity.QuestionKnowledgePoint;
import xyz.jiayuhan.contentservice.entity.QuestionFeedback;
import xyz.jiayuhan.contentservice.event.ContentAuditActions;
import xyz.jiayuhan.contentservice.event.ContentEventOutboxService;
import xyz.jiayuhan.contentservice.mapper.ContentModuleMapper;
import xyz.jiayuhan.contentservice.mapper.ErrorTypeMapper;
import xyz.jiayuhan.contentservice.mapper.KnowledgePointMapper;
import xyz.jiayuhan.contentservice.mapper.QuestionKnowledgePointMapper;
import xyz.jiayuhan.contentservice.mapper.QuestionMapper;
import xyz.jiayuhan.contentservice.mapper.QuestionFeedbackMapper;
import xyz.jiayuhan.contentservice.model.request.CreateContentModuleRequest;
import xyz.jiayuhan.contentservice.model.request.CreateErrorTypeRequest;
import xyz.jiayuhan.contentservice.model.request.CreateKnowledgePointRequest;
import xyz.jiayuhan.contentservice.model.request.CreateQuestionRequest;
import xyz.jiayuhan.contentservice.model.request.QuestionKnowledgePointBindingRequest;
import xyz.jiayuhan.contentservice.model.request.ReviewQuestionRequest;
import xyz.jiayuhan.contentservice.model.request.UpdateContentModuleRequest;
import xyz.jiayuhan.contentservice.model.request.UpdateEnabledStatusRequest;
import xyz.jiayuhan.contentservice.model.request.UpdateErrorTypeRequest;
import xyz.jiayuhan.contentservice.model.request.UpdateKnowledgePointRequest;
import xyz.jiayuhan.contentservice.model.request.UpdateQuestionRequest;
import xyz.jiayuhan.contentservice.model.response.AdminContentModuleResponse;
import xyz.jiayuhan.contentservice.model.response.AdminErrorTypeResponse;
import xyz.jiayuhan.contentservice.model.response.AdminKnowledgePointResponse;
import xyz.jiayuhan.contentservice.model.response.AdminQuestionDetailResponse;
import xyz.jiayuhan.contentservice.model.response.AdminQuestionSummaryResponse;
import xyz.jiayuhan.contentservice.model.response.QuestionKnowledgePointBindingResponse;
import xyz.jiayuhan.contentservice.model.response.ContentOperationsStatisticsResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 管理端内容维护实现。所有写操作与审计发件箱使用同一事务。 */
@Service
@RequiredArgsConstructor
public class ContentAdminServiceImpl implements ContentAdminService {

    private static final String DRAFT = "DRAFT";
    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";

    private final ContentModuleMapper contentModuleMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionMapper questionMapper;
    private final QuestionFeedbackMapper questionFeedbackMapper;
    private final QuestionKnowledgePointMapper questionKnowledgePointMapper;
    private final ErrorTypeMapper errorTypeMapper;
    private final ContentEventOutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Override
    public ContentOperationsStatisticsResponse statistics() {
        return new ContentOperationsStatisticsResponse(
                contentModuleMapper.selectCount(null),
                contentModuleMapper.selectCount(
                        new LambdaQueryWrapper<ContentModule>().eq(ContentModule::getStatus, 1)),
                knowledgePointMapper.selectCount(null),
                knowledgePointMapper.selectCount(
                        new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getStatus, 1)),
                questionMapper.selectCount(null),
                questionMapper.selectCount(
                        new LambdaQueryWrapper<Question>().eq(Question::getStatus, ENABLED)),
                questionMapper.selectCount(
                        new LambdaQueryWrapper<Question>().eq(Question::getStatus, DRAFT)),
                questionMapper.selectCount(
                        new LambdaQueryWrapper<Question>().eq(Question::getStatus, DISABLED)),
                questionFeedbackMapper.selectCount(
                        new LambdaQueryWrapper<QuestionFeedback>()
                                .in(QuestionFeedback::getStatus, List.of("PENDING", "PROCESSING"))));
    }

    @Override
    public PageResult<AdminContentModuleResponse> pageModules(
            long page,
            long size,
            String keyword,
            Integer gradeLevel,
            Integer semester,
            Integer status) {
        String normalizedKeyword = trimToNull(keyword);
        Page<ContentModule> result = contentModuleMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<ContentModule>()
                        .and(normalizedKeyword != null, wrapper -> wrapper
                                .like(ContentModule::getCode, normalizedKeyword)
                                .or()
                                .like(ContentModule::getName, normalizedKeyword)
                                .or()
                                .like(ContentModule::getLearningTheme, normalizedKeyword))
                        .eq(gradeLevel != null, ContentModule::getGradeLevel, gradeLevel)
                        .eq(semester != null, ContentModule::getSemester, semester)
                        .eq(status != null, ContentModule::getStatus, status)
                        .orderByAsc(ContentModule::getGradeLevel)
                        .orderByAsc(ContentModule::getSemester)
                        .orderByAsc(ContentModule::getSortOrder)
                        .orderByAsc(ContentModule::getId));
        return pageResult(result, result.getRecords().stream().map(this::toModuleResponse).toList());
    }

    @Override
    public AdminContentModuleResponse getModule(Long moduleId) {
        return toModuleResponse(requireModule(moduleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminContentModuleResponse createModule(CreateContentModuleRequest request) {
        ensureModuleCodeAvailable(request.code());
        ContentModule module = new ContentModule();
        module.setCode(request.code().trim());
        applyModule(module, request.name(), request.gradeLevel(), request.semester(),
                request.learningTheme(), request.publisher(), request.editionDescription(),
                request.description(), request.sortOrder());
        module.setStatus(request.status());
        module.setCreatedBy(UserContext.requireUserId());
        initializeVersionedEntity(module);
        insertWithCodeGuard(() -> contentModuleMapper.insert(module));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.MODULE_CREATED,
                "CONTENT_MODULE", module.getId(), "code=" + module.getCode());
        return toModuleResponse(module);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminContentModuleResponse updateModule(Long moduleId, UpdateContentModuleRequest request) {
        ContentModule module = requireModule(moduleId);
        verifyVersion(module, request.version());
        applyModule(module, request.name(), request.gradeLevel(), request.semester(),
                request.learningTheme(), request.publisher(), request.editionDescription(),
                request.description(), request.sortOrder());
        assertUpdated(contentModuleMapper.updateById(module));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.MODULE_UPDATED,
                "CONTENT_MODULE", moduleId, "code=" + module.getCode());
        return toModuleResponse(module);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminContentModuleResponse updateModuleStatus(
            Long moduleId, UpdateEnabledStatusRequest request) {
        ContentModule module = requireModule(moduleId);
        verifyVersion(module, request.version());
        if (request.status() == 0 && hasEnabledKnowledgePoints(moduleId)) {
            throw new BusinessException(ResultCode.CONTENT_IN_USE, "请先禁用该模块下的知识点");
        }
        module.setStatus(request.status());
        assertUpdated(contentModuleMapper.updateById(module));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.MODULE_STATUS_CHANGED,
                "CONTENT_MODULE", moduleId, "status=" + request.status());
        return toModuleResponse(module);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModule(Long moduleId, Integer version) {
        ContentModule module = requireModule(moduleId);
        verifyVersion(module, version);
        if (knowledgePointMapper.exists(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getModuleId, moduleId))) {
            throw new BusinessException(ResultCode.CONTENT_IN_USE, "模块下仍有知识点，不能删除");
        }
        assertUpdated(contentModuleMapper.update(null, new LambdaUpdateWrapper<ContentModule>()
                .eq(ContentModule::getId, moduleId)
                .eq(ContentModule::getVersion, version)
                .set(ContentModule::getDeleted, true)
                .set(ContentModule::getUpdatedAt, now())
                .set(ContentModule::getVersion, version + 1)));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.MODULE_DELETED,
                "CONTENT_MODULE", moduleId, "code=" + module.getCode());
    }

    @Override
    public List<AdminKnowledgePointResponse> listKnowledgePoints(
            Long moduleId, String keyword, Integer status) {
        String normalizedKeyword = trimToNull(keyword);
        return knowledgePointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(moduleId != null, KnowledgePoint::getModuleId, moduleId)
                        .and(normalizedKeyword != null, wrapper -> wrapper
                                .like(KnowledgePoint::getCode, normalizedKeyword)
                                .or()
                                .like(KnowledgePoint::getName, normalizedKeyword))
                        .eq(status != null, KnowledgePoint::getStatus, status)
                        .orderByAsc(KnowledgePoint::getModuleId)
                        .orderByAsc(KnowledgePoint::getSortOrder)
                        .orderByAsc(KnowledgePoint::getId))
                .stream()
                .map(this::toKnowledgePointResponse)
                .toList();
    }

    @Override
    public AdminKnowledgePointResponse getKnowledgePoint(Long knowledgePointId) {
        return toKnowledgePointResponse(requireKnowledgePoint(knowledgePointId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminKnowledgePointResponse createKnowledgePoint(CreateKnowledgePointRequest request) {
        ContentModule module = requireModule(request.moduleId());
        ensureKnowledgePointCodeAvailable(request.code());
        validateParent(request.moduleId(), null, request.parentId());
        if (request.status() == 1) {
            requireEnabledModule(module);
            requireEnabledParent(request.parentId());
        }
        KnowledgePoint knowledgePoint = new KnowledgePoint();
        knowledgePoint.setModuleId(request.moduleId());
        knowledgePoint.setCode(request.code().trim());
        applyKnowledgePoint(knowledgePoint, request.parentId(), request.name(), request.description(),
                request.difficultyLevel(), request.sortOrder());
        knowledgePoint.setStatus(request.status());
        knowledgePoint.setCreatedBy(UserContext.requireUserId());
        initializeVersionedEntity(knowledgePoint);
        insertWithCodeGuard(() -> knowledgePointMapper.insert(knowledgePoint));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.KNOWLEDGE_POINT_CREATED,
                "KNOWLEDGE_POINT", knowledgePoint.getId(), "code=" + knowledgePoint.getCode());
        return toKnowledgePointResponse(knowledgePoint);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminKnowledgePointResponse updateKnowledgePoint(
            Long knowledgePointId, UpdateKnowledgePointRequest request) {
        KnowledgePoint knowledgePoint = requireKnowledgePoint(knowledgePointId);
        verifyVersion(knowledgePoint, request.version());
        validateParent(knowledgePoint.getModuleId(), knowledgePointId, request.parentId());
        if (Integer.valueOf(1).equals(knowledgePoint.getStatus())) {
            requireEnabledParent(request.parentId());
        }
        applyKnowledgePoint(knowledgePoint, request.parentId(), request.name(), request.description(),
                request.difficultyLevel(), request.sortOrder());
        assertUpdated(knowledgePointMapper.updateById(knowledgePoint));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.KNOWLEDGE_POINT_UPDATED,
                "KNOWLEDGE_POINT", knowledgePointId, "code=" + knowledgePoint.getCode());
        return toKnowledgePointResponse(knowledgePoint);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminKnowledgePointResponse updateKnowledgePointStatus(
            Long knowledgePointId, UpdateEnabledStatusRequest request) {
        KnowledgePoint knowledgePoint = requireKnowledgePoint(knowledgePointId);
        verifyVersion(knowledgePoint, request.version());
        if (request.status() == 1) {
            requireEnabledModule(requireModule(knowledgePoint.getModuleId()));
            requireEnabledParent(knowledgePoint.getParentId());
        } else {
            ensureKnowledgePointCanBeDisabled(knowledgePointId);
        }
        knowledgePoint.setStatus(request.status());
        assertUpdated(knowledgePointMapper.updateById(knowledgePoint));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.KNOWLEDGE_POINT_STATUS_CHANGED,
                "KNOWLEDGE_POINT", knowledgePointId, "status=" + request.status());
        return toKnowledgePointResponse(knowledgePoint);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgePoint(Long knowledgePointId, Integer version) {
        KnowledgePoint knowledgePoint = requireKnowledgePoint(knowledgePointId);
        verifyVersion(knowledgePoint, version);
        if (knowledgePointMapper.exists(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getParentId, knowledgePointId))) {
            throw new BusinessException(ResultCode.CONTENT_IN_USE, "知识点仍有下级知识点，不能删除");
        }
        if (questionKnowledgePointMapper.exists(new LambdaQueryWrapper<QuestionKnowledgePoint>()
                .eq(QuestionKnowledgePoint::getKnowledgePointId, knowledgePointId))) {
            throw new BusinessException(ResultCode.CONTENT_IN_USE, "知识点已绑定题目，不能删除");
        }
        assertUpdated(knowledgePointMapper.update(null, new LambdaUpdateWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getId, knowledgePointId)
                .eq(KnowledgePoint::getVersion, version)
                .set(KnowledgePoint::getDeleted, true)
                .set(KnowledgePoint::getUpdatedAt, now())
                .set(KnowledgePoint::getVersion, version + 1)));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.KNOWLEDGE_POINT_DELETED,
                "KNOWLEDGE_POINT", knowledgePointId, "code=" + knowledgePoint.getCode());
    }

    @Override
    public PageResult<AdminQuestionSummaryResponse> pageQuestions(
            long page,
            long size,
            String keyword,
            Long knowledgePointId,
            String questionMode,
            String questionType,
            Integer difficulty,
            String status) {
        List<Long> questionIds = questionIdsForKnowledgePoint(knowledgePointId);
        if (knowledgePointId != null && questionIds.isEmpty()) {
            return new PageResult<>(page, size, 0, 0, List.of());
        }
        String normalizedKeyword = trimToNull(keyword);
        Page<Question> result = questionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Question>()
                        .in(knowledgePointId != null, Question::getId, questionIds)
                        .and(normalizedKeyword != null, wrapper -> wrapper
                                .like(Question::getCode, normalizedKeyword)
                                .or()
                                .like(Question::getStem, normalizedKeyword))
                        .eq(StringUtils.hasText(questionMode), Question::getQuestionMode, questionMode)
                        .eq(StringUtils.hasText(questionType), Question::getQuestionType, questionType)
                        .eq(difficulty != null, Question::getDifficulty, difficulty)
                        .eq(StringUtils.hasText(status), Question::getStatus, status)
                        .orderByDesc(Question::getUpdatedAt)
                        .orderByDesc(Question::getId));
        return pageResult(result, result.getRecords().stream().map(this::toQuestionSummary).toList());
    }

    @Override
    public AdminQuestionDetailResponse getQuestion(Long questionId) {
        return toQuestionDetail(requireQuestion(questionId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminQuestionDetailResponse createQuestion(CreateQuestionRequest request) {
        ensureQuestionCodeAvailable(request.code());
        List<QuestionKnowledgePointBindingRequest> bindings = validateBindings(request.knowledgePoints());
        Question question = new Question();
        question.setCode(request.code().trim());
        applyQuestion(question, request.questionMode(), request.questionType(), request.stem(),
                request.options(), request.standardAnswer(), request.analysis(), request.generationRule(),
                request.difficulty(), request.estimatedSeconds());
        question.setStatus(DRAFT);
        question.setCreatedBy(UserContext.requireUserId());
        initializeVersionedEntity(question);
        insertWithCodeGuard(() -> questionMapper.insert(question));
        replaceBindings(question.getId(), bindings);
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.QUESTION_CREATED,
                "QUESTION", question.getId(), "code=" + question.getCode());
        return toQuestionDetail(question);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminQuestionDetailResponse updateQuestion(Long questionId, UpdateQuestionRequest request) {
        Question question = requireQuestion(questionId);
        verifyVersion(question, request.version());
        List<QuestionKnowledgePointBindingRequest> bindings = validateBindings(request.knowledgePoints());
        applyQuestion(question, request.questionMode(), request.questionType(), request.stem(),
                request.options(), request.standardAnswer(), request.analysis(), request.generationRule(),
                request.difficulty(), request.estimatedSeconds());
        question.setStatus(DRAFT);
        question.setReviewedBy(null);
        question.setReviewedAt(null);
        assertUpdated(questionMapper.updateById(question));
        replaceBindings(questionId, bindings);
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.QUESTION_UPDATED,
                "QUESTION", questionId, "code=" + question.getCode() + ",status=DRAFT");
        return toQuestionDetail(question);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminQuestionDetailResponse reviewQuestion(Long questionId, ReviewQuestionRequest request) {
        Question question = requireQuestion(questionId);
        verifyVersion(question, request.version());
        String nextStatus = switch (request.decision()) {
            case "APPROVE" -> {
                if (!DRAFT.equals(question.getStatus()) && !DISABLED.equals(question.getStatus())) {
                    throw new BusinessException(ResultCode.QUESTION_STATE_INVALID);
                }
                ensureQuestionBindingsEnabled(questionId);
                yield ENABLED;
            }
            case "RETURN" -> {
                if (!DRAFT.equals(question.getStatus())) {
                    throw new BusinessException(ResultCode.QUESTION_STATE_INVALID);
                }
                yield DRAFT;
            }
            case "DISABLE" -> {
                if (!ENABLED.equals(question.getStatus())) {
                    throw new BusinessException(ResultCode.QUESTION_STATE_INVALID);
                }
                yield DISABLED;
            }
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "审核决定不正确");
        };
        question.setStatus(nextStatus);
        question.setReviewedBy(UserContext.requireUserId());
        question.setReviewedAt(now());
        assertUpdated(questionMapper.updateById(question));
        String remark = trimToNull(request.remark());
        String detail = "decision=" + request.decision()
                + (remark == null ? "" : ",remark=" + remark);
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.QUESTION_REVIEWED,
                "QUESTION", questionId, detail);
        return toQuestionDetail(question);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Long questionId, Integer version) {
        Question question = requireQuestion(questionId);
        verifyVersion(question, version);
        assertUpdated(questionMapper.update(null, new LambdaUpdateWrapper<Question>()
                .eq(Question::getId, questionId)
                .eq(Question::getVersion, version)
                .set(Question::getDeleted, true)
                .set(Question::getUpdatedAt, now())
                .set(Question::getVersion, version + 1)));
        questionKnowledgePointMapper.delete(new LambdaQueryWrapper<QuestionKnowledgePoint>()
                .eq(QuestionKnowledgePoint::getQuestionId, questionId));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.QUESTION_DELETED,
                "QUESTION", questionId, "code=" + question.getCode());
    }

    @Override
    public List<AdminErrorTypeResponse> listErrorTypes(
            String keyword, String category, String applicableQuestionType, Integer status) {
        String normalizedKeyword = trimToNull(keyword);
        return errorTypeMapper.selectList(new LambdaQueryWrapper<ErrorType>()
                        .and(normalizedKeyword != null, wrapper -> wrapper
                                .like(ErrorType::getCode, normalizedKeyword)
                                .or()
                                .like(ErrorType::getName, normalizedKeyword))
                        .eq(StringUtils.hasText(category), ErrorType::getCategory, category)
                        .eq(StringUtils.hasText(applicableQuestionType),
                                ErrorType::getApplicableQuestionType, applicableQuestionType)
                        .eq(status != null, ErrorType::getStatus, status)
                        .orderByAsc(ErrorType::getCategory)
                        .orderByAsc(ErrorType::getCode))
                .stream()
                .map(this::toErrorTypeResponse)
                .toList();
    }

    @Override
    public AdminErrorTypeResponse getErrorType(Long errorTypeId) {
        return toErrorTypeResponse(requireErrorType(errorTypeId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminErrorTypeResponse createErrorType(CreateErrorTypeRequest request) {
        ensureErrorTypeCodeAvailable(request.code());
        ErrorType errorType = new ErrorType();
        errorType.setCode(request.code().trim());
        applyErrorType(errorType, request.name(), request.category(), request.applicableQuestionType(),
                request.description(), request.explanationTemplate(), request.remediationHint());
        errorType.setStatus(request.status());
        initializeVersionedEntity(errorType);
        insertWithCodeGuard(() -> errorTypeMapper.insert(errorType));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.ERROR_TYPE_CREATED,
                "ERROR_TYPE", errorType.getId(), "code=" + errorType.getCode());
        return toErrorTypeResponse(errorType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminErrorTypeResponse updateErrorType(Long errorTypeId, UpdateErrorTypeRequest request) {
        ErrorType errorType = requireErrorType(errorTypeId);
        verifyVersion(errorType, request.version());
        applyErrorType(errorType, request.name(), request.category(), request.applicableQuestionType(),
                request.description(), request.explanationTemplate(), request.remediationHint());
        assertUpdated(errorTypeMapper.updateById(errorType));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.ERROR_TYPE_UPDATED,
                "ERROR_TYPE", errorTypeId, "code=" + errorType.getCode());
        return toErrorTypeResponse(errorType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminErrorTypeResponse updateErrorTypeStatus(
            Long errorTypeId, UpdateEnabledStatusRequest request) {
        ErrorType errorType = requireErrorType(errorTypeId);
        verifyVersion(errorType, request.version());
        errorType.setStatus(request.status());
        assertUpdated(errorTypeMapper.updateById(errorType));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.ERROR_TYPE_STATUS_CHANGED,
                "ERROR_TYPE", errorTypeId, "status=" + request.status());
        return toErrorTypeResponse(errorType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteErrorType(Long errorTypeId, Integer version) {
        ErrorType errorType = requireErrorType(errorTypeId);
        verifyVersion(errorType, version);
        assertUpdated(errorTypeMapper.update(null, new LambdaUpdateWrapper<ErrorType>()
                .eq(ErrorType::getId, errorTypeId)
                .eq(ErrorType::getVersion, version)
                .set(ErrorType::getDeleted, true)
                .set(ErrorType::getUpdatedAt, now())
                .set(ErrorType::getVersion, version + 1)));
        outboxService.appendAudit(
                UserContext.requireUserId(), ContentAuditActions.ERROR_TYPE_DELETED,
                "ERROR_TYPE", errorTypeId, "code=" + errorType.getCode());
    }

    private void applyModule(
            ContentModule module,
            String name,
            Integer gradeLevel,
            Integer semester,
            String learningTheme,
            String publisher,
            String editionDescription,
            String description,
            Integer sortOrder) {
        module.setName(name.trim());
        module.setGradeLevel(gradeLevel);
        module.setSemester(semester);
        module.setLearningTheme(learningTheme.trim());
        module.setPublisher(trimToNull(publisher));
        module.setEditionDescription(trimToNull(editionDescription));
        module.setDescription(trimToNull(description));
        module.setSortOrder(sortOrder);
    }

    private void applyKnowledgePoint(
            KnowledgePoint knowledgePoint,
            Long parentId,
            String name,
            String description,
            Integer difficultyLevel,
            Integer sortOrder) {
        knowledgePoint.setParentId(parentId);
        knowledgePoint.setName(name.trim());
        knowledgePoint.setDescription(trimToNull(description));
        knowledgePoint.setDifficultyLevel(difficultyLevel);
        knowledgePoint.setSortOrder(sortOrder);
    }

    private void applyQuestion(
            Question question,
            String questionMode,
            String questionType,
            String stem,
            String options,
            String standardAnswer,
            String analysis,
            String generationRule,
            Integer difficulty,
            Integer estimatedSeconds) {
        String normalizedOptions = normalizeJson(options, "选项");
        String normalizedAnswer = normalizeJson(standardAnswer, "标准答案");
        String normalizedRule = normalizeJson(generationRule, "生成规则");
        if ("CHOICE".equals(questionType) && normalizedOptions == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "选择题必须提供选项 JSON");
        }
        if ("TEMPLATE".equals(questionMode) && normalizedRule == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "模板题必须提供生成规则 JSON");
        }
        question.setQuestionMode(questionMode);
        question.setQuestionType(questionType);
        question.setStem(stem.trim());
        question.setOptions(normalizedOptions);
        question.setStandardAnswer(normalizedAnswer);
        question.setAnalysis(trimToNull(analysis));
        question.setGenerationRule("TEMPLATE".equals(questionMode) ? normalizedRule : null);
        question.setDifficulty(difficulty);
        question.setEstimatedSeconds(estimatedSeconds);
    }

    private void applyErrorType(
            ErrorType errorType,
            String name,
            String category,
            String applicableQuestionType,
            String description,
            String explanationTemplate,
            String remediationHint) {
        errorType.setName(name.trim());
        errorType.setCategory(category);
        errorType.setApplicableQuestionType(trimToNull(applicableQuestionType));
        errorType.setDescription(trimToNull(description));
        errorType.setExplanationTemplate(trimToNull(explanationTemplate));
        errorType.setRemediationHint(trimToNull(remediationHint));
    }

    private List<QuestionKnowledgePointBindingRequest> validateBindings(
            List<QuestionKnowledgePointBindingRequest> bindings) {
        Set<Long> ids = new HashSet<>();
        long primaryCount = 0;
        for (QuestionKnowledgePointBindingRequest binding : bindings) {
            if (!ids.add(binding.knowledgePointId())) {
                throw new BusinessException(ResultCode.CONTENT_RELATION_INVALID, "题目不能重复绑定同一知识点");
            }
            if (Boolean.TRUE.equals(binding.primary())) {
                primaryCount++;
            }
            requireKnowledgePoint(binding.knowledgePointId());
        }
        if (primaryCount != 1) {
            throw new BusinessException(ResultCode.CONTENT_RELATION_INVALID, "题目必须且只能有一个主知识点");
        }
        return List.copyOf(bindings);
    }

    private void replaceBindings(Long questionId, List<QuestionKnowledgePointBindingRequest> bindings) {
        questionKnowledgePointMapper.delete(new LambdaQueryWrapper<QuestionKnowledgePoint>()
                .eq(QuestionKnowledgePoint::getQuestionId, questionId));
        for (QuestionKnowledgePointBindingRequest binding : bindings) {
            QuestionKnowledgePoint relation = new QuestionKnowledgePoint();
            relation.setQuestionId(questionId);
            relation.setKnowledgePointId(binding.knowledgePointId());
            relation.setPrimary(binding.primary());
            relation.setWeight(binding.weight());
            questionKnowledgePointMapper.insert(relation);
        }
    }

    private void ensureQuestionBindingsEnabled(Long questionId) {
        List<QuestionKnowledgePoint> bindings = questionKnowledgePointMapper.selectList(
                new LambdaQueryWrapper<QuestionKnowledgePoint>()
                        .eq(QuestionKnowledgePoint::getQuestionId, questionId));
        if (bindings.isEmpty()) {
            throw new BusinessException(ResultCode.CONTENT_RELATION_INVALID, "题目尚未绑定知识点");
        }
        for (QuestionKnowledgePoint binding : bindings) {
            KnowledgePoint knowledgePoint = requireKnowledgePoint(binding.getKnowledgePointId());
            if (!Integer.valueOf(1).equals(knowledgePoint.getStatus())) {
                throw new BusinessException(ResultCode.CONTENT_RELATION_INVALID, "题目绑定了未启用的知识点");
            }
            requireEnabledModule(requireModule(knowledgePoint.getModuleId()));
        }
    }

    private List<Long> questionIdsForKnowledgePoint(Long knowledgePointId) {
        if (knowledgePointId == null) {
            return List.of();
        }
        return questionKnowledgePointMapper.selectList(new LambdaQueryWrapper<QuestionKnowledgePoint>()
                        .eq(QuestionKnowledgePoint::getKnowledgePointId, knowledgePointId))
                .stream()
                .map(QuestionKnowledgePoint::getQuestionId)
                .toList();
    }

    private void validateParent(Long moduleId, Long currentId, Long parentId) {
        if (parentId == null) {
            return;
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null) {
            if (!visited.add(cursor) || cursor.equals(currentId)) {
                throw new BusinessException(ResultCode.CONTENT_RELATION_INVALID, "知识点父子关系不能形成循环");
            }
            KnowledgePoint parent = requireKnowledgePoint(cursor);
            if (!moduleId.equals(parent.getModuleId())) {
                throw new BusinessException(ResultCode.CONTENT_RELATION_INVALID, "上级知识点必须属于同一内容模块");
            }
            cursor = parent.getParentId();
        }
    }

    private void requireEnabledParent(Long parentId) {
        if (parentId != null && !Integer.valueOf(1).equals(requireKnowledgePoint(parentId).getStatus())) {
            throw new BusinessException(ResultCode.CONTENT_RELATION_INVALID, "启用知识点前必须先启用上级知识点");
        }
    }

    private void ensureKnowledgePointCanBeDisabled(Long knowledgePointId) {
        if (knowledgePointMapper.exists(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getParentId, knowledgePointId)
                .eq(KnowledgePoint::getStatus, 1))) {
            throw new BusinessException(ResultCode.CONTENT_IN_USE, "请先禁用下级知识点");
        }
        List<Long> questionIds = questionIdsForKnowledgePoint(knowledgePointId);
        if (!questionIds.isEmpty() && questionMapper.exists(new LambdaQueryWrapper<Question>()
                .in(Question::getId, questionIds)
                .eq(Question::getStatus, ENABLED))) {
            throw new BusinessException(ResultCode.CONTENT_IN_USE, "请先下架引用该知识点的题目");
        }
    }

    private boolean hasEnabledKnowledgePoints(Long moduleId) {
        return knowledgePointMapper.exists(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getModuleId, moduleId)
                .eq(KnowledgePoint::getStatus, 1));
    }

    private void ensureModuleCodeAvailable(String code) {
        if (contentModuleMapper.exists(new LambdaQueryWrapper<ContentModule>()
                .eq(ContentModule::getCode, code.trim()))) {
            throw new BusinessException(ResultCode.CONTENT_CODE_EXISTS);
        }
    }

    private void ensureKnowledgePointCodeAvailable(String code) {
        if (knowledgePointMapper.exists(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getCode, code.trim()))) {
            throw new BusinessException(ResultCode.CONTENT_CODE_EXISTS);
        }
    }

    private void ensureQuestionCodeAvailable(String code) {
        if (questionMapper.exists(new LambdaQueryWrapper<Question>()
                .eq(Question::getCode, code.trim()))) {
            throw new BusinessException(ResultCode.CONTENT_CODE_EXISTS);
        }
    }

    private void ensureErrorTypeCodeAvailable(String code) {
        if (errorTypeMapper.exists(new LambdaQueryWrapper<ErrorType>()
                .eq(ErrorType::getCode, code.trim()))) {
            throw new BusinessException(ResultCode.CONTENT_CODE_EXISTS);
        }
    }

    private void insertWithCodeGuard(Runnable insertAction) {
        try {
            insertAction.run();
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.CONTENT_CODE_EXISTS);
        }
    }

    private ContentModule requireModule(Long moduleId) {
        ContentModule module = contentModuleMapper.selectById(moduleId);
        if (module == null) {
            throw new BusinessException(ResultCode.CONTENT_MODULE_NOT_FOUND);
        }
        return module;
    }

    private KnowledgePoint requireKnowledgePoint(Long knowledgePointId) {
        KnowledgePoint knowledgePoint = knowledgePointMapper.selectById(knowledgePointId);
        if (knowledgePoint == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_POINT_NOT_FOUND);
        }
        return knowledgePoint;
    }

    private Question requireQuestion(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.QUESTION_NOT_FOUND);
        }
        return question;
    }

    private ErrorType requireErrorType(Long errorTypeId) {
        ErrorType errorType = errorTypeMapper.selectById(errorTypeId);
        if (errorType == null) {
            throw new BusinessException(ResultCode.ERROR_TYPE_NOT_FOUND);
        }
        return errorType;
    }

    private void requireEnabledModule(ContentModule module) {
        if (!Integer.valueOf(1).equals(module.getStatus())) {
            throw new BusinessException(ResultCode.CONTENT_RELATION_INVALID, "所属内容模块尚未启用");
        }
    }

    private void verifyVersion(VersionedEntity entity, Integer expectedVersion) {
        if (!expectedVersion.equals(entity.getVersion())) {
            throw new BusinessException(ResultCode.CONTENT_VERSION_CONFLICT);
        }
    }

    private void assertUpdated(int affectedRows) {
        if (affectedRows != 1) {
            throw new BusinessException(ResultCode.CONTENT_VERSION_CONFLICT);
        }
    }

    private void initializeVersionedEntity(xyz.jiayuhan.common.data.model.SoftDeleteVersionedEntity entity) {
        entity.setDeleted(false);
        entity.setVersion(0);
    }

    private String normalizeJson(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            objectMapper.readTree(normalized);
            return normalized;
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + "不是合法 JSON");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private AdminContentModuleResponse toModuleResponse(ContentModule module) {
        return new AdminContentModuleResponse(
                module.getId(), module.getCode(), module.getName(), module.getGradeLevel(),
                module.getSemester(), module.getLearningTheme(), module.getPublisher(),
                module.getEditionDescription(), module.getDescription(), module.getSortOrder(),
                module.getStatus(), module.getCreatedBy(), module.getCreatedAt(), module.getUpdatedAt(),
                module.getVersion());
    }

    private AdminKnowledgePointResponse toKnowledgePointResponse(KnowledgePoint knowledgePoint) {
        return new AdminKnowledgePointResponse(
                knowledgePoint.getId(), knowledgePoint.getModuleId(), knowledgePoint.getParentId(),
                knowledgePoint.getCode(), knowledgePoint.getName(), knowledgePoint.getDescription(),
                knowledgePoint.getDifficultyLevel(), knowledgePoint.getSortOrder(),
                knowledgePoint.getStatus(), knowledgePoint.getCreatedBy(), knowledgePoint.getCreatedAt(),
                knowledgePoint.getUpdatedAt(), knowledgePoint.getVersion());
    }

    private AdminQuestionSummaryResponse toQuestionSummary(Question question) {
        return new AdminQuestionSummaryResponse(
                question.getId(), question.getCode(), question.getQuestionMode(),
                question.getQuestionType(), question.getStem(), question.getDifficulty(),
                question.getEstimatedSeconds(), question.getStatus(), question.getCreatedBy(),
                question.getReviewedBy(), question.getReviewedAt(), question.getUpdatedAt(),
                question.getVersion());
    }

    private AdminQuestionDetailResponse toQuestionDetail(Question question) {
        List<QuestionKnowledgePoint> bindings = questionKnowledgePointMapper.selectList(
                new LambdaQueryWrapper<QuestionKnowledgePoint>()
                        .eq(QuestionKnowledgePoint::getQuestionId, question.getId())
                        .orderByDesc(QuestionKnowledgePoint::getPrimary)
                        .orderByAsc(QuestionKnowledgePoint::getKnowledgePointId));
        Map<Long, KnowledgePoint> points = new HashMap<>();
        if (!bindings.isEmpty()) {
            knowledgePointMapper.selectByIds(bindings.stream()
                            .map(QuestionKnowledgePoint::getKnowledgePointId)
                            .toList())
                    .forEach(point -> points.put(point.getId(), point));
        }
        List<QuestionKnowledgePointBindingResponse> bindingResponses = bindings.stream()
                .map(binding -> {
                    KnowledgePoint point = points.get(binding.getKnowledgePointId());
                    return new QuestionKnowledgePointBindingResponse(
                            binding.getKnowledgePointId(),
                            point == null ? null : point.getCode(),
                            point == null ? null : point.getName(),
                            binding.getPrimary(),
                            binding.getWeight());
                })
                .toList();
        return new AdminQuestionDetailResponse(
                question.getId(), question.getCode(), question.getQuestionMode(),
                question.getQuestionType(), question.getStem(), question.getOptions(),
                question.getStandardAnswer(), question.getAnalysis(), question.getGenerationRule(),
                question.getDifficulty(), question.getEstimatedSeconds(), question.getStatus(),
                question.getCreatedBy(), question.getReviewedBy(), question.getReviewedAt(),
                question.getCreatedAt(), question.getUpdatedAt(), question.getVersion(), bindingResponses);
    }

    private AdminErrorTypeResponse toErrorTypeResponse(ErrorType errorType) {
        return new AdminErrorTypeResponse(
                errorType.getId(), errorType.getCode(), errorType.getName(), errorType.getCategory(),
                errorType.getApplicableQuestionType(), errorType.getDescription(),
                errorType.getExplanationTemplate(), errorType.getRemediationHint(), errorType.getStatus(),
                errorType.getCreatedAt(), errorType.getUpdatedAt(), errorType.getVersion());
    }

    private <T> PageResult<T> pageResult(Page<?> page, List<T> records) {
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }
}
