package xyz.jiayuhan.common.mq;

/** RabbitMQ 交换机、路由键和队列的统一命名约定。 */
public final class MqTopology {

    public static final String EVENT_EXCHANGE = "app.domain.events";
    public static final String DEAD_LETTER_EXCHANGE = "app.dead-letter";

    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String AUDIT_OPERATION_ROUTING_KEY = "audit.operation";

    public static final String NOTIFICATION_USER_REGISTERED_QUEUE = "notification.user-registered.v1";
    public static final String NOTIFICATION_DEAD_LETTER_QUEUE = "notification.dead-letter.v1";
    public static final String NOTIFICATION_DEAD_LETTER_ROUTING_KEY = "notification.dead-letter";

    public static final String AUDIT_USER_REGISTERED_QUEUE = "audit.user-registered.v1";
    public static final String AUDIT_OPERATION_QUEUE = "audit.operation.v1";
    public static final String AUDIT_DEAD_LETTER_QUEUE = "audit.dead-letter.v1";
    public static final String AUDIT_DEAD_LETTER_ROUTING_KEY = "audit.dead-letter";

    private MqTopology() {
    }
}
