package xyz.jiayuhan.notificationservice.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.common.mq.event.UserRegisteredEvent;
import xyz.jiayuhan.notificationservice.service.NotificationService;

/** 消费用户注册事件并异步创建欢迎站内信。 */
@Component
@RequiredArgsConstructor
public class UserRegisteredConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = MqTopology.NOTIFICATION_USER_REGISTERED_QUEUE)
    public void onUserRegistered(UserRegisteredEvent event) {
        notificationService.createWelcomeNotification(event);
    }
}
