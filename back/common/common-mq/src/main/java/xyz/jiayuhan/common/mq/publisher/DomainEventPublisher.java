package xyz.jiayuhan.common.mq.publisher;

import xyz.jiayuhan.common.mq.event.DomainEvent;

/** 向领域事件交换机可靠发布事件的统一接口。 */
public interface DomainEventPublisher {

    /**
     * 发布事件并等待 RabbitMQ Broker 确认。
     *
     * @param routingKey 事件路由键
     * @param event 不可变领域事件
     */
    void publish(String routingKey, DomainEvent event);
}
