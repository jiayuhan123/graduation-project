package xyz.jiayuhan.notificationservice.service;

import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.mq.event.UserRegisteredEvent;
import xyz.jiayuhan.notificationservice.model.NotificationVO;

import java.util.UUID;

/** 站内通知写入、查询与已读状态维护服务。 */
public interface NotificationService {

    PageResult<NotificationVO> page(Long userId, long page, long size, Boolean read);

    long unreadCount(Long userId);

    void markRead(Long userId, UUID notificationId);

    void markAllRead(Long userId);

    void createWelcomeNotification(UserRegisteredEvent event);
}
