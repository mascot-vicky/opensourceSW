package com.suwontrip.app.auth.controller;

import com.suwontrip.app.auth.dto.AuthErrorResponse;
import com.suwontrip.app.auth.exception.AuthErrorCode;
import com.suwontrip.app.auth.exception.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthSessionController.class)
public class AuthExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<AuthErrorResponse> authenticationFailure(AuthException exception) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.code().status())
                .cacheControl(CacheControl.noStore());
        if (exception.retryAfter() != null) {
            response.header(HttpHeaders.RETRY_AFTER, exception.retryAfter().toString());
        }
        return response.body(AuthErrorResponse.from(exception.code()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<AuthErrorResponse> invalidCredentials() {
        return response(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentNotValidException.class })
    public ResponseEntity<AuthErrorResponse> invalidRequest() {
        return response(AuthErrorCode.INVALID_REQUEST);
    }

    // 프레임워크가 지정한 상태는 기존 처리기에 맡기고, 예상 밖 실패만 원본 명세의 500으로 변환한다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthErrorResponse> internalFailure(Exception exception) throws Exception {
        if (exception instanceof ErrorResponse) {
            throw exception;
        }
        log.error("Unexpected session API failure", exception);
        return response(AuthErrorCode.INTERNAL_ERROR);
    }

    private static ResponseEntity<AuthErrorResponse> response(AuthErrorCode code) {
        return ResponseEntity.status(code.status()).cacheControl(CacheControl.noStore())
                .body(AuthErrorResponse.from(code));
    }
}
