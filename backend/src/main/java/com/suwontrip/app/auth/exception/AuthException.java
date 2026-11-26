package com.suwontrip.app.auth.exception;

public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final AuthErrorCode code;

    private final Long retryAfter;

    public AuthException(AuthErrorCode code) {
        this(code, null);
    }

    public AuthException(AuthErrorCode code, Long retryAfter) {
        super(code.message());
        this.code = code;
        this.retryAfter = retryAfter;
    }

    public AuthErrorCode code() {
        return code;
    }

    public Long retryAfter() {
        return retryAfter;
    }
}
