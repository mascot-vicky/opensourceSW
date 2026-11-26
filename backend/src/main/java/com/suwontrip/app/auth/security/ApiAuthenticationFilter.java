package com.suwontrip.app.auth.security;

import java.io.IOException;

import com.suwontrip.app.auth.exception.AuthErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class ApiAuthenticationFilter extends OncePerRequestFilter {
    private final AuthErrorWriter errors;

    ApiAuthenticationFilter(AuthErrorWriter errors) {
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String path = request.getServletPath();
        if (path.isEmpty()) {
            path = request.getRequestURI().substring(request.getContextPath().length());
        }

        boolean publicSessionRequest = path.equals("/api/v1/auth/session")
                && (request.getMethod().equals("GET") || request.getMethod().equals("POST"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Spring의 익명 Authentication도 isAuthenticated()가 true일 수 있어 타입을 따로 제외한다.
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (path.startsWith("/api/v1/") && !publicSessionRequest && !authenticated) {
            errors.write(response, AuthErrorCode.AUTHENTICATION_REQUIRED);
            return;
        }
        chain.doFilter(request, response);
    }
}
