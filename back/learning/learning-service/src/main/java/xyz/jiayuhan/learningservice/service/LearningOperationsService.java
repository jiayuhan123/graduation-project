package xyz.jiayuhan.learningservice.service;

import xyz.jiayuhan.learningservice.model.response.LearningOperationsStatisticsResponse;

/** 管理员学习域运营统计。 */
public interface LearningOperationsService {

    LearningOperationsStatisticsResponse statistics(Long requesterId);
}
