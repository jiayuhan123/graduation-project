package xyz.jiayuhan.common.data.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;

/** 同时支持逻辑删除和乐观锁的审计实体基类。 */
public abstract class SoftDeleteVersionedEntity extends VersionedEntity {

    @TableField("is_deleted")
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
