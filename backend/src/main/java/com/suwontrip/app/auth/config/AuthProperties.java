package com.suwontrip.app.auth.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth")
public record AuthProperties(
        Demo demo,
        int maxLoginAttempts,
        Duration loginWindow,
        int maxTrackedClients) {

    public AuthProperties {
        if (maxLoginAttempts < 1 || maxTrackedClients < 1 || loginWindow == null
                || loginWindow.isNegative() || loginWindow.compareTo(Duration.ofSeconds(1)) < 0
                || loginWindow.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("로그인 제한 횟수·저장 한도는 양수, 시간은 1초~1시간이어야 한다.");
        }

        if (demo == null) {
            throw new IllegalArgumentException("app.auth.demo 설정이 필요하다.");
        }
    }

    public record Demo(String userId, String displayName, String loginId, String passwordHash) {

        public Demo {

            List<String> values = List.of(userId == null ? "" : userId,
                    displayName == null ? "" : displayName, loginId == null ? "" : loginId,
                    passwordHash == null ? "" : passwordHash);
            boolean supplied = values.stream().anyMatch(value -> !value.isEmpty());

            if (supplied && (values.stream().anyMatch(String::isBlank) || loginId.length() > 128
                    || !passwordHash.matches("\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}"))) {
                throw new IllegalArgumentException("시연 계정 네 항목과 올바른 BCrypt 해시를 함께 설정해야 한다.");
            }
        }

        public boolean configured() {
            return loginId != null && !loginId.isEmpty();
        }

        @Override
        public String toString() {
            return "Demo[configured=" + configured() + "]";
        }
    }
}
