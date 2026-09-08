package xyz.jiayuhan.contentservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.contentapi.model.response.ContentModuleResponse;
import xyz.jiayuhan.contentapi.model.response.ErrorTypeResponse;
import xyz.jiayuhan.contentapi.model.response.KnowledgePointResponse;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.contentservice.entity.ContentModule;
import xyz.jiayuhan.contentservice.entity.ErrorType;
import xyz.jiayuhan.contentservice.entity.KnowledgePoint;
import xyz.jiayuhan.contentservice.entity.Question;
import xyz.jiayuhan.contentservice.entity.QuestionKnowledgePoint;
import xyz.jiayuhan.contentservice.mapper.ContentModuleMapper;
import xyz.jiayuhan.contentservice.mapper.ErrorTypeMapper;
import xyz.jiayuhan.contentservice.mapper.KnowledgePointMapper;
import xyz.jiayuhan.contentservice.mapper.QuestionKnowledgePointMapper;
import xyz.jiayuhan.contentservice.mapper.QuestionMapper;
import xyz.jiayuhan.contentservice.model.response.PublishedQuestionResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 基于 MyBatis-Plus 的内容目录查询实现。 */
@Service
@RequiredArgsConstructor
public class ContentCatalogServiceImpl implements ContentCatalogService {

    private final ContentModuleMapper contentModuleMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionMapper questionMapper;
    private final QuestionKnowledgePointMapper questionKnowledgePointMapper;
    private final ErrorTypeMapper errorTypeMapper;

    @Override
    public PageResult<ContentModuleResponse> pageModules(
            long page, long size, Integer gradeLevel, Integer semester, Integer status) {
        Page<ContentModule> result = contentModuleMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<ContentModule>()
                        .eq(gradeLevel != null, ContentModule::getGradeLevel, gradeLevel)
                        .eq(semester != null, ContentModule::getSemester, semester)
                        .eq(status != null, ContentModule::getStatus, status)
                        .orderByAsc(ContentModule::getGradeLevel)
                        .orderByAsc(ContentModule::getSemester)
                        .orderByAsc(ContentModule::getSortOrder)
                        .orderByAsc(ContentModule::getId));
        return pageResult(result, result.getRecords().stream().map(this::toResponse).toList());
    }

    @Override
    public ContentModuleResponse getModule(Long moduleId, boolean enabledOnly) {
        ContentModule module = contentModuleMapper.selectOne(new LambdaQueryWrapper<ContentModule>()
                .eq(ContentModule::getId, moduleId)
                .eq(enabledOnly, ContentModule::getStatus, 1));
        if (module == null) {
            throw new BusinessException(ResultCode.CONTENT_MODULE_NOT_FOUND);
        }
        return toResponse(module);
    }

    @Override
    public List<KnowledgePointResponse> listKnowledgePoints(
            Long moduleId, Long parentId, Integer status) {
        if (moduleId != null && status != null) {
            getModule(moduleId, true);
        }
        return knowledgePointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(moduleId != null, KnowledgePoint::getModuleId, moduleId)
                        .eq(parentId != null, KnowledgePoint::getParentId, parentId)
                        .eq(status != null, KnowledgePoint::getStatus, status)
                        .orderByAsc(KnowledgePoint::getSortOrder)
                        .orderByAsc(KnowledgePoint::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public KnowledgePointResponse getKnowledgePoint(Long knowledgePointId, boolean enabledOnly) {
        KnowledgePoint knowledgePoint = knowledgePointMapper.selectOne(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getId, knowledgePointId)
                        .eq(enabledOnly, KnowledgePoint::getStatus, 1));
        if (knowledgePoint == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_POINT_NOT_FOUND);
        }
        if (enabledOnly) {
            getModule(knowledgePoint.getModuleId(), true);
        }
        return toResponse(knowledgePoint);
    }

    @Override
    public PageResult<PublishedQuestionResponse> pagePublishedQuestions(
            long page,
            long size,
            Long knowledgePointId,
            String questionType,
            Integer difficulty,
            String status) {
        List<Long> questionIds = null;
        if (knowledgePointId != null) {
            questionIds = questionKnowledgePointMapper.selectList(
                            new LambdaQueryWrapper<QuestionKnowledgePoint>()
                                    .eq(QuestionKnowledgePoint::getKnowledgePointId, knowledgePointId))
                    .stream()
                    .map(QuestionKnowledgePoint::getQuestionId)
                    .toList();
            if (questionIds.isEmpty()) {
                return new PageResult<>(page, size, 0, 0, List.of());
            }
        }

        Page<Question> result = questionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Question>()
                        .in(questionIds != null, Question::getId, questionIds)
                        .eq(questionType != null, Question::getQuestionType, questionType)
                        .eq(difficulty != null, Question::getDifficulty, difficulty)
                        .eq(status != null, Question::getStatus, status)
                        .orderByDesc(Question::getUpdatedAt)
                        .orderByDesc(Question::getId));
        return pageResult(result, result.getRecords().stream().map(this::toPublishedResponse).toList());
    }

    @Override
    public PublishedQuestionResponse getPublishedQuestion(Long questionId) {
        Question question = questionMapper.selectOne(new LambdaQueryWrapper<Question>()
                .eq(Question::getId, questionId)
                .eq(Question::getStatus, "ENABLED"));
        if (question == null) {
            throw new BusinessException(ResultCode.QUESTION_NOT_FOUND);
        }
        return toPublishedResponse(question);
    }

    @Override
    public QuestionResponse getQuestionInternal(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.QUESTION_NOT_FOUND);
        }
        return toResponse(question);
    }

    @Override
    public List<QuestionResponse> getQuestions(List<Long> questionIds) {
        Map<Long, QuestionResponse> byId = new LinkedHashMap<>();
        questionMapper.selectByIds(questionIds).forEach(question ->
                byId.put(question.getId(), toResponse(question)));
        return questionIds.stream().distinct().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public PageResult<QuestionResponse> pageQuestionsInternal(
            long page,
            long size,
            String keyword,
            Long knowledgePointId,
            String questionMode,
            String questionType,
            Integer difficulty) {
        List<Long> questionIds = null;
        if (knowledgePointId != null) {
            questionIds = questionKnowledgePointMapper.selectList(
                            new LambdaQueryWrapper<QuestionKnowledgePoint>()
                                    .eq(QuestionKnowledgePoint::getKnowledgePointId, knowledgePointId))
                    .stream()
                    .map(QuestionKnowledgePoint::getQuestionId)
                    .toList();
            if (questionIds.isEmpty()) {
                return new PageResult<>(page, size, 0, 0, List.of());
            }
        }
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<Question> result = questionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Question>()
                        .in(questionIds != null, Question::getId, questionIds)
                        .and(normalizedKeyword != null, wrapper -> wrapper
                                .like(Question::getCode, normalizedKeyword)
                                .or()
                                .like(Question::getStem, normalizedKeyword))
                        .eq(StringUtils.hasText(questionMode), Question::getQuestionMode, questionMode)
                        .eq(StringUtils.hasText(questionType), Question::getQuestionType, questionType)
                        .eq(difficulty != null, Question::getDifficulty, difficulty)
                        .eq(Question::getStatus, "ENABLED")
                        .orderByDesc(Question::getUpdatedAt)
                        .orderByDesc(Question::getId));
        return pageResult(result, result.getRecords().stream().map(this::toResponse).toList());
    }

    @Override
    public List<ErrorTypeResponse> listErrorTypes(
            String category, String applicableQuestionType, Integer status) {
        return errorTypeMapper.selectList(new LambdaQueryWrapper<ErrorType>()
                        .eq(category != null, ErrorType::getCategory, category)
                        .and(applicableQuestionType != null, wrapper -> wrapper
                                .eq(ErrorType::getApplicableQuestionType, applicableQuestionType)
                                .or()
                                .isNull(ErrorType::getApplicableQuestionType))
                        .eq(status != null, ErrorType::getStatus, status)
                        .orderByAsc(ErrorType::getCategory)
                        .orderByAsc(ErrorType::getCode))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ContentModuleResponse toResponse(ContentModule module) {
        return new ContentModuleResponse(
                module.getId(), module.getCode(), module.getName(), module.getGradeLevel(),
                module.getSemester(), module.getLearningTheme(), module.getPublisher(),
                module.getEditionDescription(), module.getDescription(), module.getSortOrder(),
                module.getStatus());
    }

    private KnowledgePointResponse toResponse(KnowledgePoint knowledgePoint) {
        return new KnowledgePointResponse(
                knowledgePoint.getId(), knowledgePoint.getModuleId(), knowledgePoint.getParentId(),
                knowledgePoint.getCode(), knowledgePoint.getName(), knowledgePoint.getDescription(),
                knowledgePoint.getDifficultyLevel(), knowledgePoint.getSortOrder(),
                knowledgePoint.getStatus());
    }

    private QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.getId(), question.getCode(), question.getQuestionMode(),
                question.getQuestionType(), question.getStem(), question.getOptions(),
                question.getStandardAnswer(), question.getAnalysis(), question.getGenerationRule(),
                question.getDifficulty(), question.getEstimatedSeconds(), question.getStatus(),
                questionKnowledgePointMapper.selectList(
                                new LambdaQueryWrapper<QuestionKnowledgePoint>()
                                        .eq(QuestionKnowledgePoint::getQuestionId, question.getId())
                                        .orderByDesc(QuestionKnowledgePoint::getPrimary)
                                        .orderByAsc(QuestionKnowledgePoint::getKnowledgePointId))
                        .stream()
                        .map(QuestionKnowledgePoint::getKnowledgePointId)
                        .toList());
    }

    private PublishedQuestionResponse toPublishedResponse(Question question) {
        return new PublishedQuestionResponse(
                question.getId(), question.getCode(), question.getQuestionMode(),
                question.getQuestionType(), question.getStem(), question.getOptions(),
                question.getDifficulty(), question.getEstimatedSeconds());
    }

    private ErrorTypeResponse toResponse(ErrorType errorType) {
        return new ErrorTypeResponse(
                errorType.getId(), errorType.getCode(), errorType.getName(), errorType.getCategory(),
                errorType.getApplicableQuestionType(), errorType.getDescription(),
                errorType.getExplanationTemplate(), errorType.getRemediationHint(), errorType.getStatus());
    }

    private <T> PageResult<T> pageResult(Page<?> page, List<T> records) {
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }
}
