package xyz.jiayuhan.fileservice.service;

import org.springframework.web.multipart.MultipartFile;
import xyz.jiayuhan.fileservice.model.FileDownload;
import xyz.jiayuhan.fileservice.model.FileMetadataVO;

import java.util.UUID;

/** 当前用户私有文件的上传、查询、下载和删除服务。 */
public interface FileService {

    FileMetadataVO upload(Long ownerId, MultipartFile file);

    FileMetadataVO metadata(Long ownerId, UUID fileId);

    FileDownload download(Long ownerId, UUID fileId);

    void delete(Long ownerId, UUID fileId);
}
