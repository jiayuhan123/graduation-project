package xyz.jiayuhan.common.security;

/** 受信任内部 HTTP 调用使用的请求头。 */
public final class SecurityHeaders {

    /** OCR 工作器等内部调用使用的共享认证请求头。 */
    public static final String INTERNAL_TOKEN = "X-Internal-Token";

    private SecurityHeaders() {
    }
}
