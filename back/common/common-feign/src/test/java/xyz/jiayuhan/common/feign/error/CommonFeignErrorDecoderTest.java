package xyz.jiayuhan.common.feign.error;

import feign.Response;
import org.junit.jupiter.api.Test;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.feign.exception.RemoteServiceException;

import java.io.ByteArrayInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommonFeignErrorDecoderTest {

    @Test
    void shouldConvertRemoteHttpErrorToServiceUnavailable() throws Exception {
        Response.Body body = mock(Response.Body.class);
        when(body.asInputStream()).thenReturn(new ByteArrayInputStream("remote\nerror".getBytes(UTF_8)));
        Response response = mock(Response.class);
        when(response.status()).thenReturn(502);
        when(response.body()).thenReturn(body);

        Exception decoded = new CommonFeignErrorDecoder().decode("UserClient#getUser(Long)", response);

        assertThat(decoded).isInstanceOf(RemoteServiceException.class);
        RemoteServiceException exception = (RemoteServiceException) decoded;
        assertThat(exception.getCode()).isEqualTo(ResultCode.SERVICE_UNAVAILABLE.getCode());
        assertThat(exception.getRemoteStatus()).isEqualTo(502);
        assertThat(exception.getMethodKey()).isEqualTo("UserClient#getUser(Long)");
        assertThat(exception.getRemoteResponseBody()).isEqualTo("remote error");
    }
}
