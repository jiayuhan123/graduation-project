package xyz.jiayuhan.fileservice.storage;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import com.aliyun.sdk.service.oss2.models.GetObjectRequest;
import com.aliyun.sdk.service.oss2.models.GetObjectResult;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.fileservice.config.OssStorageProperties;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** 使用阿里云 OSS Java SDK V2 访问私有 Bucket 的对象存储实现。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AliyunOssObjectStorage implements ObjectStorage {

    private final OSSClient ossClient;
    private final OssStorageProperties properties;

    @Override
    public void put(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            ossClient.putObject(PutObjectRequest.newBuilder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .body(BinaryData.fromStream(inputStream, size))
                    .build());
        } catch (Exception exception) {
            log.error("阿里云 OSS 写入失败，objectKey={}", objectKey, exception);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "阿里云 OSS 写入失败");
        }
    }

    @Override
    public InputStream open(String objectKey) {
        try {
            GetObjectResult result = ossClient.getObject(GetObjectRequest.newBuilder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
            return new OssResultInputStream(result);
        } catch (Exception exception) {
            log.warn("阿里云 OSS 对象读取失败，objectKey={}", objectKey, exception);
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
    }

    @Override
    public void remove(String objectKey) {
        try {
            ossClient.deleteObject(DeleteObjectRequest.newBuilder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
        } catch (Exception exception) {
            log.error("阿里云 OSS 删除失败，objectKey={}", objectKey, exception);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "阿里云 OSS 删除失败");
        }
    }

    /**
     * 下载响应结束时同时关闭 SDK 结果对象，确保 HTTP 连接能够及时回收到连接池。
     */
    private static final class OssResultInputStream extends FilterInputStream {

        private final GetObjectResult result;

        private OssResultInputStream(GetObjectResult result) {
            super(result.body());
            this.result = result;
        }

        @Override
        public void close() throws IOException {
            try {
                result.close();
            } catch (Exception exception) {
                throw new IOException("关闭阿里云 OSS 下载流失败", exception);
            }
        }
    }
}
