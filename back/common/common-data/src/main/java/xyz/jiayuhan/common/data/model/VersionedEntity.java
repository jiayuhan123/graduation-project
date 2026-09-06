package xyz.jiayuhan.common.data.model;

import com.baomidou.mybatisplus.annotation.Version;

/** 带 MyBatis-Plus 乐观锁字段的审计实体基类。 */
public abstract class VersionedEntity extends AuditableEntity {

    @Version
    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
