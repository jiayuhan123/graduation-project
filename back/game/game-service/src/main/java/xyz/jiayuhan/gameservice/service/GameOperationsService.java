package xyz.jiayuhan.gameservice.service;

import xyz.jiayuhan.gameservice.model.response.GameOperationsStatisticsResponse;

/** 管理员双人 PK 运营统计。 */
public interface GameOperationsService {

    GameOperationsStatisticsResponse statistics(Long requesterId);
}
