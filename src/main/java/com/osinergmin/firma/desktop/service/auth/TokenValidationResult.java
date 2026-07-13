package com.osinergmin.firma.desktop.service.auth;

public record TokenValidationResult(boolean success, String message) {

    public static TokenValidationResult ok(String message) {
        return new TokenValidationResult(true, message);
    }

    public static TokenValidationResult fail(String message) {
        return new TokenValidationResult(false, message);
    }
}
