package xyz.jiayuhan.common.feign.config;

import feign.Feign;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import xyz.jiayuhan.common.feign.error.CommonFeignErrorDecoder;
import xyz.jiayuhan.common.feign.interceptor.FeignContextRequestInterceptor;

/** Servlet 业务服务的 OpenFeign 公共自动配置。 */
@AutoConfiguration
@ConditionalOnClass(Feign.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FeignContextRequestInterceptor.class)
    public RequestInterceptor feignContextRequestInterceptor() {
        return new FeignContextRequestInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(ErrorDecoder.class)
    public ErrorDecoder commonFeignErrorDecoder() {
        return new CommonFeignErrorDecoder();
    }
}
