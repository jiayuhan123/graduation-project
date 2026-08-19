package xyz.jiayuhan.fileservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 阿里云 OSS 连接配置。
 *
 * <p>访问密钥必须通过环境变量或配置中心注入，禁止将真实密钥写入代码仓库。</p>
 */
@Validated
@ConfigurationProperties(prefix = "app.storage.oss")
public class OssStorageProperties {

    /** OSS 区域，例如 cn-hangzhou。 */
    @NotBlank
    private String region;

    /** OSS 公网或内网访问端点，例如 https://oss-cn-hangzhou.aliyuncs.com。 */
    @NotBlank
    private String endpoint;

    /** 建议使用仅具有目标 Bucket 必要权限的 RAM 用户 AccessKey ID。 */
    @NotBlank
    private String accessKeyId;

    /** RAM 用户 AccessKey Secret，仅从安全配置注入。 */
    @NotBlank
    private String accessKeySecret;

    /** 已提前创建且访问控制为私有的 Bucket 名称。 */
    @NotBlank
    private String bucket;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
