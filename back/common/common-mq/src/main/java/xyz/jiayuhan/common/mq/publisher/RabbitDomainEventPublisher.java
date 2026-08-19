package xyz.jiayuhan.common.mq.publisher;

import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.common.mq.config.CommonMqProperties;
import xyz.jiayuhan.common.mq.event.DomainEvent;
import xyz.jiayuhan.common.mq.exception.EventPublishException;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 使用 RabbitTemplate 发布事件并同步等待 Broker 确认。 */
public class RabbitDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final CommonMqProperties properties;

    public RabbitDomainEventPublisher(RabbitTemplate rabbitTemplate, CommonMqProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        // mandatory=true 使无法路由的消息返回生产者，而不是被交换机静默丢弃。
        this.rabbitTemplate.setMandatory(true);
    }

    @Override
    public void publish(String routingKey, DomainEvent event) {
        CorrelationData correlation = new CorrelationData(event.eventId().toString());
        rabbitTemplate.convertAndSend(
                MqTopology.EVENT_EXCHANGE,
                routingKey,
                event,
                message -> {
                    message.getMessageProperties().setMessageId(event.eventId().toString());
                    message.getMessageProperties().setType(event.eventType());
                    message.getMessageProperties().setTimestamp(Date.from(event.occurredAt()));
                    message.getMessageProperties().setHeader("x-event-source", event.source());
                    return message;
                },
                correlation);

        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(
                    properties.getConfirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            ReturnedMessage returned = correlation.getReturned();
            if (!confirm.ack()) {
                throw new EventPublishException("RabbitMQ 拒绝事件：" + confirm.reason());
            }
            if (returned != null) {
                throw new EventPublishException(
                        "事件无法路由到队列：replyCode=" + returned.getReplyCode()
                                + ", replyText=" + returned.getReplyText());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EventPublishException("等待 RabbitMQ 确认时线程被中断", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new EventPublishException("等待 RabbitMQ 确认失败", exception);
        }
    }
}
