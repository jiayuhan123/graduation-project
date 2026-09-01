package xyz.jiayuhan.userservice.handler;

import cn.dev33.satoken.exception.SaTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.web.handler.GlobalExceptionHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExceptionHandlerHttpStatusTest {

    private final GlobalExceptionHandler commonHandler = new GlobalExceptionHandler();
    private final UserServiceExceptionHandler userHandler = new UserServiceExceptionHandler();

    @Test
    void shouldKeepBusinessCodeAndReturnSemanticHttpStatus() {
        ResponseEntity<Result<Void>> response = commonHandler.handleBusinessException(
                new BusinessException(ResultCode.USERNAME_EXISTS));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ResultCode.USERNAME_EXISTS.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldReturnServerErrorForUnknownException() {
        ResponseEntity<Result<Void>> response = commonHandler.handleException(new IllegalStateException("detail"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ResultCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().getCode());
        assertEquals(ResultCode.INTERNAL_SERVER_ERROR.getMessage(), response.getBody().getMessage());
    }

    @Test
    void shouldReturnPayloadTooLargeForOversizedUpload() {
        ResponseEntity<Result<Void>> response = commonHandler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(1024, null));

        assertEquals(413, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ResultCode.FILE_SIZE_EXCEEDED.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldReturnUnauthorizedForSaTokenFailure() {
        ResponseEntity<Result<Void>> response = userHandler.handleSaToken(new SaTokenException("invalid token"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldReturnConflictForDatabaseConstraintViolation() {
        ResponseEntity<Result<Void>> response = userHandler.handleDataIntegrity(
                new DataIntegrityViolationException("duplicate"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ResultCode.CONFLICT.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldPreserveFrameworkHttpStatus() {
        ResponseEntity<Result<Void>> response = commonHandler.handleErrorResponseException(
                new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getCode());
        assertEquals("请求路径不存在", response.getBody().getMessage());
    }
}
