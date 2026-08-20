package xyz.jiayuhan.common.feign.error;

import feign.Response;
import feign.codec.ErrorDecoder;
import xyz.jiayuhan.common.feign.exception.RemoteServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** 将下游非 2xx HTTP 响应转换为稳定的服务不可用异常。 */
public final class CommonFeignErrorDecoder implements ErrorDecoder {

    private static final int MAX_RESPONSE_BODY_BYTES = 2048;

    @Override
    public Exception decode(String methodKey, Response response) {
        return new RemoteServiceException(methodKey, response.status(), readResponseBody(response));
    }

    private String readResponseBody(Response response) {
        if (response.body() == null) {
            return "";
        }

        try (InputStream inputStream = response.body().asInputStream()) {
            byte[] bytes = inputStream.readNBytes(MAX_RESPONSE_BODY_BYTES);
            return normalize(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            return "";
        }
    }

    private String normalize(String body) {
        return body.replaceAll("[\\r\\n\\t]+", " ").strip();
    }
}
