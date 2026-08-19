package xyz.jiayuhan.fileservice.storage;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.GetObjectRequest;
import com.aliyun.sdk.service.oss2.models.GetObjectResult;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.fileservice.config.OssStorageProperties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证文件存储适配器对 OSS SDK V2 请求的映射与下载资源释放。 */
@ExtendWith(MockitoExtension.class)
class AliyunOssObjectStorageTest {

    @Mock
    private OSSClient ossClient;

    @Mock
    private GetObjectResult getObjectResult;

    private AliyunOssObjectStorage objectStorage;

    @BeforeEach
    void setUp() {
        OssStorageProperties properties = new OssStorageProperties();
        properties.setBucket("private-bucket");
        objectStorage = new AliyunOssObjectStorage(ossClient, properties);
    }

    @Test
    void shouldMapUploadToPrivateBucket() {
        objectStorage.put(
                "42/file-id/report.pdf",
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                3,
                "application/pdf");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(ossClient).putObject(captor.capture());
        PutObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo("private-bucket");
        assertThat(request.key()).isEqualTo("42/file-id/report.pdf");
        assertThat(request.contentType()).isEqualTo("application/pdf");
        assertThat(request.body().getLength()).isEqualTo(3L);
    }

    @Test
    void shouldCloseSdkResultWhenDownloadStreamCloses() throws Exception {
        InputStream body = new ByteArrayInputStream(new byte[]{1});
        when(getObjectResult.body()).thenReturn(body);
        when(ossClient.getObject(any(GetObjectRequest.class))).thenReturn(getObjectResult);

        try (InputStream ignored = objectStorage.open("42/file-id/report.pdf")) {
            assertThat(ignored.read()).isEqualTo(1);
        }

        verify(getObjectResult).close();
    }
}
