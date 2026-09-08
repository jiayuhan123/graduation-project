package xyz.jiayuhan.contentservice.service;

import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.contentapi.model.response.ContentModuleResponse;
import xyz.jiayuhan.contentapi.model.response.ErrorTypeResponse;
import xyz.jiayuhan.contentapi.model.response.KnowledgePointResponse;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.contentservice.model.response.PublishedQuestionResponse;

import java.util.List;

/** 内容目录的只读应用服务；内容变更命令应在审核流程确定后单独实现。 */
public interface ContentCatalogService {

    PageResult<ContentModuleResponse> pageModules(
            long page, long size, Integer gradeLevel, Integer semester, Integer status);

    ContentModuleResponse getModule(Long moduleId, boolean enabledOnly);

    List<KnowledgePointResponse> listKnowledgePoints(
            Long moduleId, Long parentId, Integer status);

    KnowledgePointResponse getKnowledgePoint(Long knowledgePointId, boolean enabledOnly);

    PageResult<PublishedQuestionResponse> pagePublishedQuestions(
            long page,
            long size,
            Long knowledgePointId,
            String questionType,
            Integer difficulty,
            String status);

    PublishedQuestionResponse getPublishedQuestion(Long questionId);

    QuestionResponse getQuestionInternal(Long questionId);

    List<QuestionResponse> getQuestions(List<Long> questionIds);

    PageResult<QuestionResponse> pageQuestionsInternal(
            long page,
            long size,
            String keyword,
            Long knowledgePointId,
            String questionMode,
            String questionType,
            Integer difficulty);

    List<ErrorTypeResponse> listErrorTypes(
            String category, String applicableQuestionType, Integer status);
}
