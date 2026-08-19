package xyz.jiayuhan.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.jiayuhan.common.mq.MqTopology;

/** 通知服务拥有的用户注册队列、绑定和死信队列。 */
@Configuration
public class NotificationMqConfig {

    @Bean
    public Queue notificationUserRegisteredQueue() {
        return QueueBuilder.durable(MqTopology.NOTIFICATION_USER_REGISTERED_QUEUE)
                .deadLetterExchange(MqTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MqTopology.NOTIFICATION_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding notificationUserRegisteredBinding(
            Queue notificationUserRegisteredQueue,
            TopicExchange domainEventExchange) {
        return BindingBuilder.bind(notificationUserRegisteredQueue)
                .to(domainEventExchange)
                .with(MqTopology.USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(MqTopology.NOTIFICATION_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding notificationDeadLetterBinding(
            Queue notificationDeadLetterQueue,
            DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(notificationDeadLetterQueue)
                .to(deadLetterExchange)
                .with(MqTopology.NOTIFICATION_DEAD_LETTER_ROUTING_KEY);
    }
}
