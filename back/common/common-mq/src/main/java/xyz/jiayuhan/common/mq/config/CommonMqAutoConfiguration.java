package xyz.jiayuhan.common.mq.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.common.mq.publisher.DomainEventPublisher;
import xyz.jiayuhan.common.mq.publisher.RabbitDomainEventPublisher;

/**
 * RabbitMQ 公共自动配置。
 *
 * <p>公共层只声明跨服务交换机；业务队列、绑定和死信路由由各消费者服务自行拥有。</p>
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
@EnableConfigurationProperties(CommonMqProperties.class)
public class CommonMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter rabbitMessageConverter() {
        // Spring AMQP 4 使用 Jackson 3 转换器，与 Spring Boot 4 的 JSON 栈保持一致。
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public TopicExchange domainEventExchange() {
        return new TopicExchange(MqTopology.EVENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(MqTopology.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public DomainEventPublisher domainEventPublisher(
            RabbitTemplate rabbitTemplate,
            CommonMqProperties properties) {
        return new RabbitDomainEventPublisher(rabbitTemplate, properties);
    }
}
