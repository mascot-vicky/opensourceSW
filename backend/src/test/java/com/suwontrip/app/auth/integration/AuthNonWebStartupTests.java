package com.suwontrip.app.auth.integration;

import com.suwontrip.app.SuwonTripApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.SecurityFilterChain;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class AuthNonWebStartupTests {
    @Test
    void nonWebCommandsStartWithoutServletSecurityAndKeepAuthenticationServices() {
        SpringApplication application = new SpringApplication(SuwonTripApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        try (var context = application.run(
                "--spring.datasource.url=jdbc:h2:mem:auth_nonweb_" + UUID.randomUUID(),
                "--spring.datasource.username=sa", "--spring.datasource.password=",
                "--logging.level.root=OFF", "--spring.main.banner-mode=off")) {
            assertThat(context.getBeansOfType(SecurityFilterChain.class)).isEmpty();
            assertThat(context.getBeansOfType(AuthenticationManager.class)).hasSize(1);
        }
    }
}
