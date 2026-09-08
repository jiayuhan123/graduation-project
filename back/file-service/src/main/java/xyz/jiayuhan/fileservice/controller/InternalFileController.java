package xyz.jiayuhan.fileservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.fileservice.model.FileDownload;
import xyz.jiayuhan.fileservice.service.FileService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** 仅供携带内部服务令牌的 OCR 工作器读取待识别原图。 */
@RestController
@RequestMapping("/internal/files")
@RequiredArgsConstructor
public class InternalFileController {

    private final FileService fileService;

    @GetMapping("/{fileId}")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID fileId,
            @RequestParam Long ownerId) {
        FileDownload file = fileService.downloadInternal(ownerId, fileId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.sizeBytes())
                .body(new InputStreamResource(file.inputStream()));
    }
}
