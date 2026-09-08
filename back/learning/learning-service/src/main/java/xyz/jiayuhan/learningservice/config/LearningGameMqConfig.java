package xyz.jiayuhan.learningservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.jiayuhan.common.mq.MqTopology;

/** 学习服务拥有的游戏事件队列和死信路由。 */
@Configuration
public class LearningGameMqConfig {

    @Bean
    public Queue learningGameEventQueue() {
        return QueueBuilder.durable(MqTopology.LEARNING_GAME_EVENT_QUEUE)
                .deadLetterExchange(MqTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MqTopology.LEARNING_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding learningGameAnswerBinding(
            Queue learningGameEventQueue,
            TopicExchange domainEventExchange) {
        return BindingBuilder.bind(learningGameEventQueue)
                .to(domainEventExchange)
                .with(MqTopology.GAME_ANSWER_GRADED_ROUTING_KEY);
    }

    @Bean
    public Binding learningGameSessionBinding(
            Queue learningGameEventQueue,
            TopicExchange domainEventExchange) {
        return BindingBuilder.bind(learningGameEventQueue)
                .to(domainEventExchange)
                .with(MqTopology.GAME_SESSION_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Queue learningDeadLetterQueue() {
        return QueueBuilder.durable(MqTopology.LEARNING_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding learningDeadLetterBinding(
            Queue learningDeadLetterQueue,
            DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(learningDeadLetterQueue)
                .to(deadLetterExchange)
                .with(MqTopology.LEARNING_DEAD_LETTER_ROUTING_KEY);
    }
}
