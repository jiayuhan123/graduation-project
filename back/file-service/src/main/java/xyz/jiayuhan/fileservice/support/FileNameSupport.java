package xyz.jiayuhan.fileservice.support;

import java.util.UUID;

/** 文件名清理与不可猜测对象键生成工具。 */
public final class FileNameSupport {

    private FileNameSupport() {
    }

    /**
     * 移除客户端路径和控制字符，只保留安全展示名。
     *
     * @param originalName 客户端文件名
     * @return 最大 255 字符的安全文件名
     */
    public static String sanitize(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "unnamed-file";
        }
        String normalized = originalName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (fileName.isBlank()) {
            return "unnamed-file";
        }
        return fileName.length() <= 255 ? fileName : fileName.substring(fileName.length() - 255);
    }

    /** 对象键不复用客户端路径，防止路径穿越和同名覆盖。 */
    public static String objectKey(Long ownerId, UUID fileId, String safeName) {
        return ownerId + "/" + fileId + "/" + safeName;
    }
}
