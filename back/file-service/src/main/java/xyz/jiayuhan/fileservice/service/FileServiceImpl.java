package xyz.jiayuhan.fileservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.fileservice.entity.FileObject;
import xyz.jiayuhan.fileservice.mapper.FileObjectMapper;
import xyz.jiayuhan.fileservice.model.FileDownload;
import xyz.jiayuhan.fileservice.model.FileMetadataVO;
import xyz.jiayuhan.fileservice.storage.ObjectStorage;
import xyz.jiayuhan.fileservice.support.FileNameSupport;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** 阿里云 OSS 对象内容与 PostgreSQL 元数据协同的文件服务实现。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FileObjectMapper fileObjectMapper;
    private final ObjectStorage objectStorage;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileMetadataVO upload(Long ownerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }

        UUID fileId = UUID.randomUUID();
        String safeName = FileNameSupport.sanitize(file.getOriginalFilename());
        String objectKey = FileNameSupport.objectKey(ownerId, fileId, safeName);
        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType() : DEFAULT_CONTENT_TYPE;
        String sha256 = sha256(file);

        try (InputStream inputStream = file.getInputStream()) {
            objectStorage.put(objectKey, inputStream, file.getSize(), contentType);
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "读取上传文件失败");
        }

        FileObject entity = new FileObject();
        entity.setId(fileId);
        entity.setOwnerId(ownerId);
        entity.setOriginalName(safeName);
        entity.setObjectKey(objectKey);
        entity.setContentType(contentType);
        entity.setSizeBytes(file.getSize());
        entity.setSha256(sha256);
        entity.setDeleted(false);
        try {
            fileObjectMapper.insert(entity);
        } catch (RuntimeException exception) {
            // 元数据落库失败时尽力回收刚写入的对象，避免产生无主文件。
            try {
                objectStorage.remove(objectKey);
            } catch (RuntimeException cleanupException) {
                // 保留原始数据库异常，同时记录补偿失败，方便运维按 objectKey 清理孤儿对象。
                exception.addSuppressed(cleanupException);
                log.error("文件元数据入库失败且 OSS 补偿删除失败，objectKey={}", objectKey, cleanupException);
            }
            throw exception;
        }
        return toMetadata(entity);
    }

    @Override
    public FileMetadataVO metadata(Long ownerId, UUID fileId) {
        return toMetadata(requireOwnedFile(ownerId, fileId));
    }

    @Override
    public FileDownload download(Long ownerId, UUID fileId) {
        FileObject file = requireOwnedFile(ownerId, fileId);
        return new FileDownload(
                objectStorage.open(file.getObjectKey()),
                file.getOriginalName(),
                file.getContentType(),
                file.getSizeBytes());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long ownerId, UUID fileId) {
        FileObject file = requireOwnedFile(ownerId, fileId);
        // 先删除对象再逻辑删除元数据；失败时保留元数据，便于运维重试。
        objectStorage.remove(file.getObjectKey());
        fileObjectMapper.deleteById(fileId);
    }

    private FileObject requireOwnedFile(Long ownerId, UUID fileId) {
        FileObject file = fileObjectMapper.selectOne(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getId, fileId)
                .eq(FileObject::getOwnerId, ownerId));
        if (file == null) {
            // 不区分不存在和不属于当前用户，避免通过响应枚举他人文件 ID。
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        return file;
    }

    private String sha256(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             DigestInputStream digestStream = new DigestInputStream(
                     inputStream, MessageDigest.getInstance("SHA-256"))) {
            digestStream.transferTo(OutputStreamSink.INSTANCE);
            return HexFormat.of().formatHex(digestStream.getMessageDigest().digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "计算文件摘要失败");
        }
    }

    private FileMetadataVO toMetadata(FileObject file) {
        return new FileMetadataVO(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getSha256(),
                file.getCreatedAt());
    }

    /** 丢弃摘要计算过程中读取的字节，避免把完整文件加载到内存。 */
    private static final class OutputStreamSink extends java.io.OutputStream {
        private static final OutputStreamSink INSTANCE = new OutputStreamSink();

        @Override
        public void write(int value) {
        }

        @Override
        public void write(byte[] buffer, int offset, int length) {
        }
    }
}
