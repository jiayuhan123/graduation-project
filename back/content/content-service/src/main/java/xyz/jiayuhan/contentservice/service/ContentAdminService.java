package xyz.jiayuhan.contentservice.service;

import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.contentservice.model.request.CreateContentModuleRequest;
import xyz.jiayuhan.contentservice.model.request.CreateErrorTypeRequest;
import xyz.jiayuhan.contentservice.model.request.CreateKnowledgePointRequest;
import xyz.jiayuhan.contentservice.model.request.CreateQuestionRequest;
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

import java.util.List;

/** 管理端内容维护应用服务。 */
public interface ContentAdminService {

    PageResult<AdminContentModuleResponse> pageModules(
            long page, long size, String keyword, Integer gradeLevel, Integer semester, Integer status);

    AdminContentModuleResponse getModule(Long moduleId);

    AdminContentModuleResponse createModule(CreateContentModuleRequest request);

    AdminContentModuleResponse updateModule(Long moduleId, UpdateContentModuleRequest request);

    AdminContentModuleResponse updateModuleStatus(Long moduleId, UpdateEnabledStatusRequest request);

    void deleteModule(Long moduleId, Integer version);

    List<AdminKnowledgePointResponse> listKnowledgePoints(
            Long moduleId, String keyword, Integer status);

    AdminKnowledgePointResponse getKnowledgePoint(Long knowledgePointId);

    AdminKnowledgePointResponse createKnowledgePoint(CreateKnowledgePointRequest request);

    AdminKnowledgePointResponse updateKnowledgePoint(Long knowledgePointId, UpdateKnowledgePointRequest request);

    AdminKnowledgePointResponse updateKnowledgePointStatus(
            Long knowledgePointId, UpdateEnabledStatusRequest request);

    void deleteKnowledgePoint(Long knowledgePointId, Integer version);

    PageResult<AdminQuestionSummaryResponse> pageQuestions(
            long page,
            long size,
            String keyword,
            Long knowledgePointId,
            String questionMode,
            String questionType,
            Integer difficulty,
            String status);

    AdminQuestionDetailResponse getQuestion(Long questionId);

    AdminQuestionDetailResponse createQuestion(CreateQuestionRequest request);

    AdminQuestionDetailResponse updateQuestion(Long questionId, UpdateQuestionRequest request);

    AdminQuestionDetailResponse reviewQuestion(Long questionId, ReviewQuestionRequest request);

    void deleteQuestion(Long questionId, Integer version);

    List<AdminErrorTypeResponse> listErrorTypes(
            String keyword, String category, String applicableQuestionType, Integer status);

    AdminErrorTypeResponse getErrorType(Long errorTypeId);

    AdminErrorTypeResponse createErrorType(CreateErrorTypeRequest request);

    AdminErrorTypeResponse updateErrorType(Long errorTypeId, UpdateErrorTypeRequest request);

    AdminErrorTypeResponse updateErrorTypeStatus(Long errorTypeId, UpdateEnabledStatusRequest request);

    void deleteErrorType(Long errorTypeId, Integer version);
}
