package xyz.jiayuhan.aiservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/** AI 模块基础设施配置。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {

    @Bean
    public RestClient pythonAgentRestClient(RestClient.Builder builder, AiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                // Uvicorn 的 HTTP/1.1 服务不接受 JDK 客户端默认发起的明文 h2c 升级；
                // 固定协议可避免升级失败时流式 POST 的请求体被丢弃。
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getStreamTimeout().plusSeconds(5));
        return builder
                .baseUrl(properties.getAgentBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
