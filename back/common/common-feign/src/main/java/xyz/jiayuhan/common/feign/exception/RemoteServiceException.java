package xyz.jiayuhan.common.feign.exception;

import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

/** OpenFeign 调用下游服务收到非 2xx HTTP 响应时抛出的异常。 */
public final class RemoteServiceException extends BusinessException {

    private final String methodKey;
    private final int remoteStatus;
    private final String remoteResponseBody;

    public RemoteServiceException(String methodKey, int remoteStatus, String remoteResponseBody) {
        super(ResultCode.SERVICE_UNAVAILABLE);
        this.methodKey = methodKey;
        this.remoteStatus = remoteStatus;
        this.remoteResponseBody = remoteResponseBody;
    }

    public String getMethodKey() {
        return methodKey;
    }

    public int getRemoteStatus() {
        return remoteStatus;
    }

    public String getRemoteResponseBody() {
        return remoteResponseBody;
    }
}
