package com.suwontrip.app.auth.exception;

import org.springframework.http.HttpStatus;

public enum AuthErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "로그인 요청 형식을 확인해 주세요."),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "로그인 정보를 확인해 주세요."),

    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요."),

    CSRF_INVALID(HttpStatus.FORBIDDEN, "요청 확인 값을 다시 받아주세요."),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근할 수 없어요."),

    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 로그인해 주세요."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요."),

    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "로그인 서비스를 사용할 수 없어요.");

    private final HttpStatus status;
    private final String message;

    AuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
