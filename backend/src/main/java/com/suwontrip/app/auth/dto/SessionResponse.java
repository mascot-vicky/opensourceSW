package com.suwontrip.app.auth.dto;

public record SessionResponse(boolean authenticated, User user, String csrfToken) {

    public record User(String id, String displayName) { }
}
