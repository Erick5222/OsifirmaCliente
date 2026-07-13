package com.osinergmin.firma.desktop.core.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

/** Extrae roles del payload JWT (sin validar firma; el token ya fue validado por API). */
public final class JwtRoleExtractor {

    private JwtRoleExtractor() {}

    public static Set<String> extractRoles(String accessToken) {
        Set<String> roles = new LinkedHashSet<>();
        if (accessToken == null || accessToken.isBlank()) {
            return roles;
        }
        String token = accessToken.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return roles;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            JsonObject payload = JsonParser.parseString(new String(decoded, StandardCharsets.UTF_8)).getAsJsonObject();
            collectRealmRoles(payload, roles);
            collectResourceRoles(payload, roles);
        } catch (RuntimeException ex) {
            return roles;
        }
        return roles;
    }

    private static void collectRealmRoles(JsonObject payload, Set<String> roles) {
        JsonObject realmAccess = payload.getAsJsonObject("realm_access");
        if (realmAccess == null) {
            return;
        }
        JsonArray realmRoles = realmAccess.getAsJsonArray("roles");
        addRoleNames(realmRoles, roles);
    }

    private static void collectResourceRoles(JsonObject payload, Set<String> roles) {
        JsonObject resourceAccess = payload.getAsJsonObject("resource_access");
        if (resourceAccess == null) {
            return;
        }
        for (String client : resourceAccess.keySet()) {
            JsonElement clientElement = resourceAccess.get(client);
            if (!clientElement.isJsonObject()) {
                continue;
            }
            JsonArray clientRoles = clientElement.getAsJsonObject().getAsJsonArray("roles");
            addRoleNames(clientRoles, roles);
        }
    }

    private static void addRoleNames(JsonArray roleArray, Set<String> roles) {
        if (roleArray == null) {
            return;
        }
        for (JsonElement element : roleArray) {
            if (element.isJsonPrimitive()) {
                String role = element.getAsString();
                if (role != null && !role.isBlank()) {
                    roles.add(role.trim());
                }
            }
        }
    }

    private static String padBase64(String value) {
        int remainder = value.length() % 4;
        if (remainder == 0) {
            return value;
        }
        return value + "====".substring(remainder);
    }
}
