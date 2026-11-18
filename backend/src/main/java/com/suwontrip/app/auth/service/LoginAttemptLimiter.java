package com.suwontrip.app.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.suwontrip.app.auth.config.AuthProperties;
import com.suwontrip.app.auth.exception.AuthErrorCode;
import com.suwontrip.app.auth.exception.AuthException;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptLimiter {

    private final Map<String, Window> clients = new HashMap<>();
    private final AuthProperties properties;
    private final Clock clock;

    public LoginAttemptLimiter(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized void acquire(String clientAddress) {

        Instant now = clock.instant();

        clients.values().removeIf(window -> !now.isBefore(window.endsAt));

        Window window = clients.get(clientAddress);
        if (window == null) {

            if (clients.size() >= properties.maxTrackedClients()) {
                throw new AuthException(AuthErrorCode.SERVICE_UNAVAILABLE);
            }

            window = new Window(now.plus(properties.loginWindow()));
            clients.put(clientAddress, window);
        }

        if (window.attempts >= properties.maxLoginAttempts()) {
            long millis = java.time.Duration.between(now, window.endsAt).toMillis();
            long retryAfter = Math.max(1, (millis + 999) / 1000);
            throw new AuthException(AuthErrorCode.TOO_MANY_ATTEMPTS, retryAfter);
        }

        window.attempts++;
    }

    private static final class Window {
        private final Instant endsAt;
        private int attempts;

        private Window(Instant endsAt) {
            this.endsAt = endsAt;
        }
    }
}
