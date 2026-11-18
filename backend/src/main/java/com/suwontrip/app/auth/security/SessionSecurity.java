package com.suwontrip.app.auth.security;

import com.suwontrip.app.auth.dto.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionSecurity {
    private final SecurityContextRepository contexts;
    private final HttpSessionCsrfTokenRepository tokens;
    private final CsrfTokenRequestHandler tokenHandler;
    private final ChangeSessionIdAuthenticationStrategy sessionIds = new ChangeSessionIdAuthenticationStrategy();
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public SessionSecurity(
            SecurityContextRepository contexts,
            HttpSessionCsrfTokenRepository tokens,
            CsrfTokenRequestHandler tokenHandler) {

        this.contexts = contexts;
        this.tokens = tokens;
        this.tokenHandler = tokenHandler;
    }

    public SessionResponse current(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SessionResponse.User user = authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof DemoUserDetails demo
                        ? new SessionResponse.User(demo.userId(), demo.displayName())
                        : null;
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return new SessionResponse(user != null, user, token.getToken());
    }

    public void establish(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {

        sessionIds.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        contexts.saveContext(context, request, response);
        replaceToken(request, response);
    }

    public void invalidate(HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());

        replaceToken(request, response);
    }

    private void replaceToken(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = tokens.generateToken(request);
        tokens.saveToken(token, request, response);
        tokenHandler.handle(request, response, () -> token);
    }
}
