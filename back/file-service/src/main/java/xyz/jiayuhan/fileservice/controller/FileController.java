package xyz.jiayuhan.fileservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.fileservice.model.FileDownload;
import xyz.jiayuhan.fileservice.model.FileMetadataVO;
import xyz.jiayuhan.fileservice.service.FileService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** 当前用户私有文件接口，所有对象访问都校验文件所有权。 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传私有文件")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileMetadataVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", fileService.upload(UserContext.requireUserId(), file));
    }

    @Operation(summary = "查询文件元数据")
    @GetMapping("/{fileId}/metadata")
    public Result<FileMetadataVO> metadata(@PathVariable UUID fileId) {
        return Result.success(fileService.metadata(UserContext.requireUserId(), fileId));
    }

    @Operation(summary = "下载私有文件")
    @GetMapping("/{fileId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID fileId) {
        FileDownload file = fileService.download(UserContext.requireUserId(), fileId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.sizeBytes())
                .body(new InputStreamResource(file.inputStream()));
    }

    @Operation(summary = "删除私有文件")
    @DeleteMapping("/{fileId}")
    public Result<Void> delete(@PathVariable UUID fileId) {
        fileService.delete(UserContext.requireUserId(), fileId);
        return Result.success();
    }
}
