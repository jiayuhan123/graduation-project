package xyz.jiayuhan.monomer.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Qwen-VL-OCR 调用与本地工作器轮询配置。 */
@ConfigurationProperties(prefix = "app.ocr")
public class OcrProperties {

    private boolean enabled;
    private String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private String apiKey;
    private String modelName = "qwen-vl-ocr-latest";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(90);
    private long fixedDelayMs = 5000;
    private int batchSize = 2;
    private int maxItems = 100;
    private int maxOutputTokens = 4096;
    private int minPixels = 3072;
    private int maxPixels = 8_388_608;
    private long maxImageBytes = 14_000_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public int getMinPixels() {
        return minPixels;
    }

    public void setMinPixels(int minPixels) {
        this.minPixels = minPixels;
    }

    public int getMaxPixels() {
        return maxPixels;
    }

    public void setMaxPixels(int maxPixels) {
        this.maxPixels = maxPixels;
    }

    public long getMaxImageBytes() {
        return maxImageBytes;
    }

    public void setMaxImageBytes(long maxImageBytes) {
        this.maxImageBytes = maxImageBytes;
    }
}
