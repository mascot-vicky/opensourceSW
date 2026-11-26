package com.suwontrip.app.auth.dto;

import java.util.List;

import com.suwontrip.app.auth.exception.AuthErrorCode;

public record AuthErrorResponse(String code, String message, List<?> errors) {

    public static AuthErrorResponse from(AuthErrorCode code) {
        return new AuthErrorResponse(code.name(), code.message(), List.of());
    }
}
