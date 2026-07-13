package com.osinergmin.firma.desktop.core.auth;

import com.osinergmin.firma.desktop.config.ClientConfiguration;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/** Determina si el usuario actual puede abrir el panel de administracion de firma. */
public final class AdminAccessPolicy {

    private AdminAccessPolicy() {}

    public static boolean hasAdminAccess() {
        return AuthSession.getAccessToken()
                .map(token -> hasAdminAccess(token, ClientConfiguration.getAdminJwtRoles()))
                .orElse(false);
    }

    public static boolean hasAdminAccess(String accessToken, Set<String> configuredAdminRoles) {
        if (configuredAdminRoles == null || configuredAdminRoles.isEmpty()) {
            return false;
        }
        Set<String> tokenRoles = JwtRoleExtractor.extractRoles(accessToken);
        for (String required : configuredAdminRoles) {
            if (required == null || required.isBlank()) {
                continue;
            }
            String normalized = required.trim().toLowerCase(Locale.ROOT);
            for (String tokenRole : tokenRoles) {
                if (tokenRole != null && tokenRole.trim().toLowerCase(Locale.ROOT).equals(normalized)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Set<String> configuredAdminRoles() {
        return ClientConfiguration.getAdminJwtRoles();
    }

    public static Set<String> currentTokenRoles() {
        return AuthSession.getAccessToken().map(JwtRoleExtractor::extractRoles).orElse(Set.of());
    }
}
