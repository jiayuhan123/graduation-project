package xyz.jiayuhan.monomer.ocr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/** OCR 模型客户端配置；关闭 OCR 时不创建外部调用客户端。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OcrProperties.class)
public class OcrConfiguration {

    @Bean("qwenOcrRestClient")
    @ConditionalOnProperty(prefix = "app.ocr", name = "enabled", havingValue = "true")
    public RestClient qwenOcrRestClient(RestClient.Builder builder, OcrProperties properties) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("启用 OCR 时必须配置 AI_OCR_API_KEY");
        }
        if (!StringUtils.hasText(properties.getEndpoint())) {
            throw new IllegalStateException("启用 OCR 时必须配置 AI_OCR_ENDPOINT");
        }
        if (!StringUtils.hasText(properties.getModelName())) {
            throw new IllegalStateException("启用 OCR 时必须配置 AI_OCR_MODEL_NAME");
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return builder
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
