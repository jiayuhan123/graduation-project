package xyz.jiayuhan.userservice.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 为用户库的 TIMESTAMPTZ 字段统一填充 UTC 时间。 */
@Configuration
public class UserServiceDataConfig {

    @Bean
    public MetaObjectHandler userServiceMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, now);
                strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(
                        metaObject,
                        "updatedAt",
                        OffsetDateTime.class,
                        OffsetDateTime.now(ZoneOffset.UTC));
            }
        };
    }
}
