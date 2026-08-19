package xyz.jiayuhan.common.data.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MyBatis-Plus 公共数据层配置项。 */
@ConfigurationProperties(prefix = "app.data")
public class CommonDataProperties {

    /** 单页允许返回的最大记录数，防止超大分页拖垮数据库。 */
    private long maxPageSize = 100L;

    /** 页码超过总页数时是否回到第一页；默认返回空页，避免产生隐式跳转。 */
    private boolean overflow;

    public long getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(long maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public boolean isOverflow() {
        return overflow;
    }

    public void setOverflow(boolean overflow) {
        this.overflow = overflow;
    }
}
