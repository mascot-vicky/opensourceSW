package com.suwontrip.app.auth.service;

import com.suwontrip.app.auth.config.AuthProperties;
import com.suwontrip.app.auth.dto.LoginRequest;
import com.suwontrip.app.auth.dto.SessionResponse;
import com.suwontrip.app.auth.exception.AuthErrorCode;
import com.suwontrip.app.auth.exception.AuthException;
import com.suwontrip.app.auth.security.SessionSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {
    private final AuthProperties properties;
    private final LoginAttemptLimiter limiter;
    private final AuthenticationManager authenticationManager;
    private final SessionSecurity sessions;

    public AuthSessionService(AuthProperties properties, LoginAttemptLimiter limiter,
                              AuthenticationManager authenticationManager, SessionSecurity sessions) {
        this.properties = properties;
        this.limiter = limiter;
        this.authenticationManager = authenticationManager;
        this.sessions = sessions;
    }

    public SessionResponse current(HttpServletRequest request) {
        return sessions.current(request);
    }

    public SessionResponse login(LoginRequest credentials, HttpServletRequest request,
                                 HttpServletResponse response) {

        if (!properties.demo().configured()) {
            throw new AuthException(AuthErrorCode.SERVICE_UNAVAILABLE);
        }

        limiter.acquire(request.getRemoteAddr());

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(credentials.loginId(), credentials.password()));

        sessions.establish(authentication, request, response);
        return sessions.current(request);
    }

    public SessionResponse logout(HttpServletRequest request, HttpServletResponse response) {
        sessions.invalidate(request, response);
        return sessions.current(request);
    }
}
