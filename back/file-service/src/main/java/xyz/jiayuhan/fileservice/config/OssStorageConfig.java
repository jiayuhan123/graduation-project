package xyz.jiayuhan.fileservice.config;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 阿里云 OSS Java SDK V2 客户端配置。 */
@Configuration
@EnableConfigurationProperties(OssStorageProperties.class)
public class OssStorageConfig {

    /**
     * 创建线程安全的 OSS 客户端，并在 Spring 容器关闭时释放连接资源。
     *
     * <p>Bucket 必须由运维提前创建并设为私有，应用不会在启动时扩大 RAM 权限去创建 Bucket。</p>
     */
    @Bean(destroyMethod = "close")
    public OSSClient ossClient(OssStorageProperties properties) {
        return OSSClient.newBuilder()
                .region(properties.getRegion())
                .endpoint(properties.getEndpoint())
                .credentialsProvider(new StaticCredentialsProvider(
                        properties.getAccessKeyId(),
                        properties.getAccessKeySecret()))
                .build();
    }
}
