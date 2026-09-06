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

class WechatWebClientTest {

    private MockRestServiceServer server;
    private WechatWebClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        WechatWebProperties properties = new WechatWebProperties();
        properties.setEnabled(true);
        properties.setAppId("web-app-id");
        properties.setSecret("web-secret");
        properties.setRedirectUri("https://math.example.com/login/wechat/callback");
        client = new WechatWebClient(builder, properties, new ObjectMapper());
    }

    @Test
    void shouldBuildOfficialAuthorizationUrl() {
        String url = client.authorizationUrl("random-state");

        assertThat(url)
                .startsWith("https://open.weixin.qq.com/connect/qrconnect?")
                .contains("appid=web-app-id")
                .contains("scope=snsapi_login")
                .contains("state=random-state")
                .endsWith("#wechat_redirect");
    }

    @Test
    void shouldExchangeCodeForCrossPlatformIdentity() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/sns/oauth2/access_token"))
                .andRespond(withSuccess(
                        "{\"access_token\":\"not-stored\",\"openid\":\"web-openid\",\"unionid\":\"unionid-1\"}",
                        MediaType.TEXT_PLAIN));

        assertThat(client.exchangeIdentity("temporary-code"))
                .isEqualTo(new WechatIdentity("web-openid", "unionid-1"));
        server.verify();
    }

    @Test
    void shouldRejectWechatErrorResponse() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/sns/oauth2/access_token"))
                .andRespond(withSuccess(
                        "{\"errcode\":40029,\"errmsg\":\"invalid code\"}",
                        MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.exchangeIdentity("invalid-code"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.WECHAT_CODE_INVALID.getCode()));
        server.verify();
    }
}
