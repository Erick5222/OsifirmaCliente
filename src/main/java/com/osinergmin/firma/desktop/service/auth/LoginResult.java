package com.osinergmin.firma.desktop.service.auth;

public record LoginResult(boolean success, String message, String accessToken, String refreshToken) {

    public static LoginResult ok(String message, String accessToken, String refreshToken) {
        return new LoginResult(true, message, accessToken, refreshToken);
    }

    public static LoginResult fail(String message) {
        return new LoginResult(false, message, null, null);
    }
}
