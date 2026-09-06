package xyz.jiayuhan.common.data.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import xyz.jiayuhan.common.data.typehandler.PostgreSqlUuidTypeHandler;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * MyBatis-Plus 公共自动配置。
 *
 * <p>项目统一使用 PostgreSQL，因此分页方言在公共层固定；业务服务只需引用
 * {@code common-data} 即可获得分页保护、乐观锁和审计字段填充。</p>
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
@EnableConfigurationProperties(CommonDataProperties.class)
public class CommonDataAutoConfiguration {

    /** 注册 PostgreSQL 分页和乐观锁插件。 */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(CommonDataProperties properties) {
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        pagination.setMaxLimit(properties.getMaxPageSize());
        pagination.setOverflow(properties.isOverflow());

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(pagination);
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    /** PostgreSQL 原生 UUID 需要显式处理器，避免 MyBatis 在无 JdbcType 时拒绝绑定。 */
    @Bean
    @ConditionalOnMissingBean(name = "postgresqlUuidTypeHandlerCustomizer")
    public ConfigurationCustomizer postgresqlUuidTypeHandlerCustomizer() {
        return configuration -> configuration
                .getTypeHandlerRegistry()
                .register(UUID.class, PostgreSqlUuidTypeHandler.class);
    }

    /** 为带有 MyBatis-Plus 填充注解的实体统一维护审计时间。 */
    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler auditMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                fillInsertTime(metaObject, "createdAt");
                fillInsertTime(metaObject, "updatedAt");
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                if (!metaObject.hasSetter("updatedAt")) {
                    return;
                }
                if (OffsetDateTime.class.equals(metaObject.getSetterType("updatedAt"))) {
                    strictUpdateFill(
                            metaObject,
                            "updatedAt",
                            OffsetDateTime.class,
                            OffsetDateTime.now(ZoneOffset.UTC));
                    return;
                }
                strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }

            private void fillInsertTime(MetaObject metaObject, String fieldName) {
                if (!metaObject.hasSetter(fieldName)) {
                    return;
                }
                if (OffsetDateTime.class.equals(metaObject.getSetterType(fieldName))) {
                    strictInsertFill(
                            metaObject,
                            fieldName,
                            OffsetDateTime.class,
                            OffsetDateTime.now(ZoneOffset.UTC));
                    return;
                }
                strictInsertFill(metaObject, fieldName, LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
