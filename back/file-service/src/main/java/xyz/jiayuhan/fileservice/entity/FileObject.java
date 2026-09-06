package xyz.jiayuhan.fileservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 私有文件元数据；真实文件内容保存在阿里云 OSS 私有 Bucket。 */
@Data
@TableName("file_object")
public class FileObject {

    /** 对外暴露的随机文件 ID，不使用可枚举自增值。 */
    @TableId(type = IdType.INPUT)
    private UUID id;

    /** 文件所有者用户 ID。 */
    private Long ownerId;

    /** 上传时的安全文件名，仅用于展示和下载响应头。 */
    private String originalName;

    /** 私有 Bucket 内不可猜测的对象键。 */
    private String objectKey;

    /** MIME 类型；无法识别时为 application/octet-stream。 */
    private String contentType;

    /** 文件字节数。 */
    private Long sizeBytes;

    /** 文件内容 SHA-256，便于完整性校验和后续去重。 */
    private String sha256;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /** 逻辑删除标记，常规查询自动排除已删除元数据。 */
    @TableField("is_deleted")
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}
