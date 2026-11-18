package com.suwontrip.app.auth.security;

import com.suwontrip.app.auth.config.AuthProperties;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DemoUserDetailsService implements UserDetailsService {
    private final AuthProperties properties;

    public DemoUserDetailsService(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) {
        AuthProperties.Demo demo = properties.demo();

        if (!demo.configured() || !demo.loginId().equals(loginId)) {
            throw new UsernameNotFoundException("계정 정보를 확인해 주세요.");
        }

        // 요청마다 새 객체를 만들어 인증 과정에서 자격 증명이 지워져도 설정값은 유지
        return new DemoUserDetails(demo);
    }
}
