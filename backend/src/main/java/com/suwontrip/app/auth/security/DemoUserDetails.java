package com.suwontrip.app.auth.security;

import com.suwontrip.app.auth.config.AuthProperties;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

final class DemoUserDetails extends User {

    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String displayName;

    DemoUserDetails(AuthProperties.Demo demo) {
        super(demo.loginId(), demo.passwordHash(), AuthorityUtils.createAuthorityList("ROLE_USER"));
        this.userId = demo.userId();
        this.displayName = demo.displayName();
    }

    String userId() {
        return userId;
    }

    String displayName() {
        return displayName;
    }
}
