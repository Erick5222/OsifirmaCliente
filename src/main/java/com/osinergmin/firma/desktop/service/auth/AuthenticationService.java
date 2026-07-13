package com.osinergmin.firma.desktop.service.auth;

import com.osinergmin.firma.desktop.core.auth.AuthSession;
import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;

public final class AuthenticationService {

    private static volatile boolean forceLogin;

    private final AuthApiClient apiClient = new AuthApiClient();

    public TokenValidationResult validateParametriaToken(InvokeConfig config) {
        if (config == null || !config.hasToken()) {
            return TokenValidationResult.fail("No hay token en parametria.");
        }
        return apiClient.validateToken(config.getToken());
    }

    public void applyParametriaSession(InvokeConfig config) {
        if (config != null && config.hasToken()) {
            AuthSession.applyTokens(config.getToken(), config.getRefreshToken());
        }
    }

    public boolean isServiceReachable() {
        return apiClient.isServiceReachable();
    }

    public LoginResult login(String username, String password) {
        LoginResult result = apiClient.login(username, password);
        if (result.success()) {
            forceLogin = false;
            AuthSession.applyTokens(result.accessToken(), result.refreshToken());
        }
        return result;
    }

    public void logout() {
        forceLogin = true;
        AuthSession.clear();
    }
}
