package xyz.jiayuhan.common.mq.exception;

/** RabbitMQ 未在超时时间内确认事件时抛出的基础设施异常。 */
public class EventPublishException extends RuntimeException {

    public EventPublishException(String message) {
        super(message);
    }

    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
