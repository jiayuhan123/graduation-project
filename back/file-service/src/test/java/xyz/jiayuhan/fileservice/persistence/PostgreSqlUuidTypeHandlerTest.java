package xyz.jiayuhan.fileservice.persistence;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.type.TypeHandler;
import org.junit.jupiter.api.Test;
import xyz.jiayuhan.common.data.config.CommonDataAutoConfiguration;
import xyz.jiayuhan.common.data.typehandler.PostgreSqlUuidTypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证公共数据配置能够为文件元数据的 PostgreSQL UUID 参数和结果注册处理器。 */
class PostgreSqlUuidTypeHandlerTest {

    @Test
    void shouldRegisterUuidTypeHandlerWithoutJdbcType() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        ConfigurationCustomizer customizer = new CommonDataAutoConfiguration()
                .postgresqlUuidTypeHandlerCustomizer();

        customizer.customize(configuration);

        TypeHandler<UUID> handler = configuration.getTypeHandlerRegistry().getTypeHandler(UUID.class);
        assertThat(handler).isInstanceOf(PostgreSqlUuidTypeHandler.class);
    }

    @Test
    void shouldWriteNativeUuidAndReadUuidValues() throws Exception {
        PostgreSqlUuidTypeHandler handler = new PostgreSqlUuidTypeHandler();
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        UUID uuid = UUID.fromString("00000000-0000-4000-8000-000000000001");
        when(resultSet.getObject("id")).thenReturn(uuid);

        handler.setParameter(statement, 1, uuid, null);

        verify(statement).setObject(1, uuid);
        assertThat(handler.getResult(resultSet, "id")).isEqualTo(uuid);
    }
}
