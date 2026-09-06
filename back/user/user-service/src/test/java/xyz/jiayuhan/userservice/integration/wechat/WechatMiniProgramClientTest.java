package xyz.jiayuhan.userservice.integration.wechat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WechatMiniProgramClientTest {

    private MockRestServiceServer server;
    private WechatMiniProgramClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        WechatMiniProgramProperties properties = new WechatMiniProgramProperties();
        properties.setEnabled(true);
        properties.setAppId("test-app-id");
        properties.setSecret("test-secret");
        properties.setCodeSessionUrl("https://api.weixin.qq.com/sns/jscode2session");
        client = new WechatMiniProgramClient(builder, properties, new ObjectMapper());
    }

    @Test
    void shouldParseSuccessfulTextPlainResponse() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/sns/jscode2session"))
                .andRespond(withSuccess(
                        "{\"openid\":\"openid-1\",\"session_key\":\"must-not-be-used\"}",
                        MediaType.TEXT_PLAIN));

        assertThat(client.exchangeIdentity("temporary-code"))
                .isEqualTo(new WechatIdentity("openid-1", null));
        server.verify();
    }

    @Test
    void shouldMapWechatErrorFromTextPlainResponse() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/sns/jscode2session"))
                .andRespond(withSuccess(
                        "{\"errcode\":40029,\"errmsg\":\"invalid code\"}",
                        MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.exchangeIdentity("invalid-code"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.WECHAT_CODE_INVALID.getCode()));
        server.verify();
    }

    @Test
    void shouldRejectMalformedResponseAsUnavailable() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/sns/jscode2session"))
                .andRespond(withSuccess("not-json", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.exchangeIdentity("temporary-code"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.SERVICE_UNAVAILABLE.getCode()));
        server.verify();
    }
}
