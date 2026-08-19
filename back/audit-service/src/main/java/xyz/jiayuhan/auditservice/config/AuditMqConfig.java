package xyz.jiayuhan.auditservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.jiayuhan.common.mq.MqTopology;

/** 审计服务拥有的领域事件队列、绑定和共享死信队列。 */
@Configuration
public class AuditMqConfig {

    @Bean
    public Queue auditUserRegisteredQueue() {
        return durableAuditQueue(MqTopology.AUDIT_USER_REGISTERED_QUEUE);
    }

    @Bean
    public Queue auditOperationQueue() {
        return durableAuditQueue(MqTopology.AUDIT_OPERATION_QUEUE);
    }

    @Bean
    public Binding auditUserRegisteredBinding(
            Queue auditUserRegisteredQueue,
            TopicExchange domainEventExchange) {
        return BindingBuilder.bind(auditUserRegisteredQueue)
                .to(domainEventExchange)
                .with(MqTopology.USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Binding auditOperationBinding(
            Queue auditOperationQueue,
            TopicExchange domainEventExchange) {
        return BindingBuilder.bind(auditOperationQueue)
                .to(domainEventExchange)
                .with(MqTopology.AUDIT_OPERATION_ROUTING_KEY);
    }

    @Bean
    public Queue auditDeadLetterQueue() {
        return QueueBuilder.durable(MqTopology.AUDIT_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding auditDeadLetterBinding(
            Queue auditDeadLetterQueue,
            DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(auditDeadLetterQueue)
                .to(deadLetterExchange)
                .with(MqTopology.AUDIT_DEAD_LETTER_ROUTING_KEY);
    }

    private Queue durableAuditQueue(String queueName) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(MqTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MqTopology.AUDIT_DEAD_LETTER_ROUTING_KEY)
                .build();
    }
}
