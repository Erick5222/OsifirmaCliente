package com.osinergmin.firma.desktop.config;

import com.osinergmin.firma.desktop.integration.invoke.SigningParam;

/** Resuelve configuracion efectiva: parametria > panel admin > empaquetado. */
public final class SigningAdminSettings {

    private SigningAdminSettings() {}

    public static String effectiveTslUrl() {
        return SigningAdminConfigStore.load().tslUrl();
    }

    public static boolean effectiveVerifyTsl() {
        return SigningAdminConfigStore.load().verifyTsl();
    }

    public static String effectiveWebTsa(SigningParam param) {
        if (param != null && param.webTsa() != null && !param.webTsa().isBlank()) {
            return param.webTsa().trim();
        }
        return SigningAdminConfigStore.load().tsaUrl();
    }

    public static String effectiveUserTsa(SigningParam param) {
        if (param != null && param.userTsa() != null && !param.userTsa().isBlank()) {
            return param.userTsa().trim();
        }
        return SigningAdminConfigStore.load().tsaUser();
    }

    public static String effectivePasswordTsa(SigningParam param) {
        if (param != null && param.passwordTsa() != null && !param.passwordTsa().isBlank()) {
            return param.passwordTsa().trim();
        }
        return SigningAdminConfigStore.load().tsaPassword();
    }

    public static String defaultSignatureLevelCode() {
        String code = SigningAdminConfigStore.load().signatureLevel();
        return code == null || code.isBlank() ? "B" : code.trim().toUpperCase();
    }

    public static String effectiveSignatureLevel(SigningParam param) {
        if (param != null && param.signatureLevel() != null && !param.signatureLevel().isBlank()) {
            return param.signatureLevel().trim().toUpperCase();
        }
        return defaultSignatureLevelCode();
    }

    public static int effectiveSignatureLevelCode(SigningParam param) {
        String level = effectiveSignatureLevel(param);
        return switch (level) {
            case "T" -> 2;
            case "LTA" -> 3;
            default -> 1;
        };
    }
}
