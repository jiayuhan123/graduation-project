package xyz.jiayuhan.common.web.config;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommonJsonAutoConfigurationTest {

    @Test
    void serializesLongAsStringWithoutChangingInteger() {
        JsonMapper.Builder builder = JsonMapper.builder();
        new CommonJsonAutoConfiguration().longToStringJsonMapperBuilderCustomizer().customize(builder);
        JsonMapper mapper = builder.build();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", 9_223_372_036_854_775_000L);
        payload.put("status", 1);

        assertThat(mapper.writeValueAsString(payload))
                .isEqualTo("{\"id\":\"9223372036854775000\",\"status\":1}");
        assertThat(mapper.writeValueAsString(new Page(1L, 20L)))
                .isEqualTo("{\"page\":1,\"size\":20}");
    }

    private record Page(long page, long size) {
    }
}
