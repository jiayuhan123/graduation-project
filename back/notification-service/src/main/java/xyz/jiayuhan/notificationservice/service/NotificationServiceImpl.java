package xyz.jiayuhan.notificationservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.mq.event.UserRegisteredEvent;
import xyz.jiayuhan.notificationservice.entity.Notification;
import xyz.jiayuhan.notificationservice.mapper.NotificationMapper;
import xyz.jiayuhan.notificationservice.model.NotificationVO;

import java.time.OffsetDateTime;
import java.util.UUID;

/** PostgreSQL 站内通知实现，并以来源事件 ID 保证 RabbitMQ 消费幂等。 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public PageResult<NotificationVO> page(Long userId, long page, long size, Boolean read) {
        Page<Notification> result = notificationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(read != null, Notification::getRead, read)
                        .orderByDesc(Notification::getCreatedAt));
        return new PageResult<>(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                result.getRecords().stream().map(this::toVO).toList());
    }

    @Override
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getRead, false));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, UUID notificationId) {
        Notification notification = requireOwnedNotification(userId, notificationId);
        if (Boolean.TRUE.equals(notification.getRead())) {
            return;
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .set(Notification::getRead, true)
                .set(Notification::getReadAt, OffsetDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getRead, false)
                .set(Notification::getRead, true)
                .set(Notification::getReadAt, OffsetDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createWelcomeNotification(UserRegisteredEvent event) {
        boolean consumed = notificationMapper.exists(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getSourceEventId, event.eventId()));
        if (consumed) {
            // RabbitMQ 至少一次投递可能产生重复消息，已落库的事件直接确认即可。
            return;
        }

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(event.userId());
        notification.setType("SYSTEM");
        notification.setTitle("欢迎使用");
        notification.setContent("账号 " + event.username() + " 已注册成功，欢迎使用本系统。");
        notification.setRead(false);
        notification.setSourceEventId(event.eventId());
        notificationMapper.insert(notification);
    }

    private Notification requireOwnedNotification(Long userId, UUID notificationId) {
        Notification notification = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId));
        if (notification == null) {
            throw new BusinessException(ResultCode.NOTIFICATION_NOT_FOUND);
        }
        return notification;
    }

    private NotificationVO toVO(Notification notification) {
        return new NotificationVO(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                Boolean.TRUE.equals(notification.getRead()),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
