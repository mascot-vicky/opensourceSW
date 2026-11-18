package com.suwontrip.app.auth.security;

import com.suwontrip.app.auth.exception.AuthErrorCode;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

@Configuration
public class AuthSecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    HttpSessionCsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }

    @Bean
    CsrfTokenRequestHandler csrfTokenRequestHandler() {
        return new XorCsrfTokenRequestAttributeHandler();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts,
            HttpSessionCsrfTokenRepository tokens, CsrfTokenRequestHandler tokenHandler,
            AuthErrorWriter errors) throws Exception {

        // 로그인 성공 시 SessionSecurity가 인증 상태를 명시적으로 저장해야 다음 요청에 유지된다.
        http.securityContext(context -> context.securityContextRepository(contexts).requireExplicitSave(true))

                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                                "/api/v1/auth/session")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/session").permitAll()
                        .anyRequest().authenticated())

                .csrf(csrf -> csrf.csrfTokenRepository(tokens).csrfTokenRequestHandler(tokenHandler))

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> errors.write(response,
                                AuthErrorCode.AUTHENTICATION_REQUIRED))
                        .accessDeniedHandler((request, response, exception) -> errors.write(response,
                                exception instanceof CsrfException ? AuthErrorCode.CSRF_INVALID
                                        : AuthErrorCode.ACCESS_DENIED)))

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)

                // 보호된 API에서는 CSRF 검사보다 먼저 미인증 여부를 판단해 401을 우선 반환한다.
                .addFilterBefore(new ApiAuthenticationFilter(errors), CsrfFilter.class);
        return http.build();
    }
}
