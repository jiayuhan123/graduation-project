package xyz.jiayuhan.fileservice.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 文件元数据响应。
 *
 * @param id 文件 ID
 * @param originalName 原始文件名
 * @param contentType MIME 类型
 * @param sizeBytes 文件字节数
 * @param sha256 内容摘要
 * @param createdAt 上传时间
 */
public record FileMetadataVO(
        UUID id,
        String originalName,
        String contentType,
        long sizeBytes,
        String sha256,
        OffsetDateTime createdAt
) {
}
