package com.suwontrip.app.auth.security;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suwontrip.app.auth.dto.AuthErrorResponse;
import com.suwontrip.app.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class AuthErrorWriter {
    private final ObjectMapper objectMapper;

    public AuthErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, AuthErrorCode code) throws IOException {

        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.setCharacterEncoding("UTF-8");

        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        objectMapper.writeValue(response.getOutputStream(), AuthErrorResponse.from(code));
    }
}
