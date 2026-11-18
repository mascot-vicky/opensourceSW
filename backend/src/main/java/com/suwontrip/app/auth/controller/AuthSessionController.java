package com.suwontrip.app.auth.controller;

import com.suwontrip.app.auth.dto.LoginRequest;
import com.suwontrip.app.auth.dto.SessionResponse;
import com.suwontrip.app.auth.service.AuthSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/session")
public class AuthSessionController {
    private final AuthSessionService sessions;

    public AuthSessionController(AuthSessionService sessions) {
        this.sessions = sessions;
    }

    @GetMapping
    public ResponseEntity<SessionResponse> current(HttpServletRequest request) {
        return noStore(sessions.current(request));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest credentials,
            HttpServletRequest request, HttpServletResponse response) {
        return noStore(sessions.login(credentials, request, response));
    }

    @DeleteMapping
    public ResponseEntity<SessionResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        return noStore(sessions.logout(request, response));
    }

    private static ResponseEntity<SessionResponse> noStore(SessionResponse body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
