package xyz.jiayuhan.fileservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.fileservice.model.FileDownload;
import xyz.jiayuhan.fileservice.service.FileReviewAuthorizationService;
import xyz.jiayuhan.fileservice.service.FileService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** 学生本人或有权限的教师、管理员以内联方式预览 OCR 图片。 */
@Tag(name = "拍照复核文件")
@RestController
@RequestMapping("/api/files/reviews")
@RequiredArgsConstructor
public class FileReviewController {

    private final FileService fileService;
    private final FileReviewAuthorizationService authorizationService;

    @Operation(summary = "预览拍照批改原图或切题图片")
    @GetMapping("/{fileId}")
    public ResponseEntity<InputStreamResource> preview(
            @PathVariable UUID fileId,
            @RequestParam Long ownerId) {
        authorizationService.authorize(UserContext.requireUserId(), ownerId);
        FileDownload file = fileService.downloadInternal(ownerId, fileId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.sizeBytes())
                .body(new InputStreamResource(file.inputStream()));
    }
}
