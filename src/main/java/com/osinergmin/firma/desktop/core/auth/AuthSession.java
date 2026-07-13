package com.osinergmin.firma.desktop.core.auth;

import java.util.Optional;

public final class AuthSession {

    private static volatile String accessToken;
    private static volatile String refreshToken;

    private AuthSession() {
    }

    public static void clear() {
        accessToken = null;
        refreshToken = null;
    }

    public static void applyTokens(String access, String refresh) {
        accessToken = access;
        refreshToken = refresh;
    }

    public static Optional<String> getAccessToken() {
        return Optional.ofNullable(accessToken).filter(s -> !s.isBlank());
    }

    public static boolean isAuthenticated() {
        return getAccessToken().isPresent();
    }
}
