package xyz.jiayuhan.fileservice.model;

import java.io.InputStream;

/** 文件下载流及生成 HTTP 响应所需的元数据。 */
public record FileDownload(
        InputStream inputStream,
        String originalName,
        String contentType,
        long sizeBytes
) {
}
