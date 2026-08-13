package xyz.jiayuhan.common.redis.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 统一序列化配置
 * <p>
 * Key 使用 String 序列化，Value 使用 Jackson 3 JSON 序列化（内置类型信息，支持 LocalDateTime），
 * 供各业务服务直接注入 {@code RedisTemplate<String, Object>} 使用（如预约名额原子扣减等场景）。
 * 通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 自动装配。
 * </p>
 */
@AutoConfiguration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Spring Data Redis 4 新版 JSON 序列化器（Jackson 3，自动携带类型信息）
        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.create(builder -> {
        });
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Key / HashKey 使用字符串序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        // Value / HashValue 使用 JSON 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
