package xyz.jiayuhan.common.web.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/** Web API 的统一 JSON 契约配置。 */
@AutoConfiguration
@ConditionalOnClass(JsonMapper.class)
public class CommonJsonAutoConfiguration {

    /**
     * 将包装类型 {@link Long} 输出为 JSON 字符串，避免雪花 ID 超过 JavaScript 安全整数范围后丢失精度。
     * 分页页码等 primitive long 以及 Integer 等普通数值仍保持 JSON number。
     */
    @Bean
    @ConditionalOnMissingBean(name = "longToStringJsonMapperBuilderCustomizer")
    public JsonMapperBuilderCustomizer longToStringJsonMapperBuilderCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("long-to-string");
            module.addSerializer(Long.class, new ToStringSerializer(Long.class));
            builder.addModule(module);
        };
    }
}
