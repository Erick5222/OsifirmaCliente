package com.osinergmin.firma.desktop.core.auth;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Persiste la preferencia «Recordarme» del login (solo usuario, nunca contraseña).
 */
public final class LoginRememberMeStore {

    private static final String PREF_ENABLED = "remember.enabled";
    private static final String PREF_USERNAME = "remember.username";

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(LoginRememberMeStore.class);

    private LoginRememberMeStore() {}

    public static boolean isEnabled() {
        return PREFS.getBoolean(PREF_ENABLED, false);
    }

    public static Optional<String> getSavedUsername() {
        if (!isEnabled()) {
            return Optional.empty();
        }
        String username = PREFS.get(PREF_USERNAME, "").trim();
        return username.isEmpty() ? Optional.empty() : Optional.of(username);
    }

    public static void save(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.isEmpty()) {
            clear();
            return;
        }
        PREFS.putBoolean(PREF_ENABLED, true);
        PREFS.put(PREF_USERNAME, normalized);
    }

    public static void clear() {
        PREFS.remove(PREF_ENABLED);
        PREFS.remove(PREF_USERNAME);
    }
}
