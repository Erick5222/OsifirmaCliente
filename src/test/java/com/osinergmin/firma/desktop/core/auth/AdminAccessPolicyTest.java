package com.osinergmin.firma.desktop.core.auth;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAccessPolicyTest {

    private static final String SAMPLE_TOKEN =
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ0ZXN0In0."
                    + "eyJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJkZWZhdWx0LXJvbGVzLW9zaW5lcmdtaW4iXX0sInJlc291cmNlX2FjY2VzcyI6eyJyZWFsbS1tYW5hZ2VtZW50Ijp7InJvbGVzIjpbInZpZXctY2xpZW50cyIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyJdfX19."
                    + "signature";

    @Test
    void reconoceRolesRealmManagementComoAdmin() {
        assertTrue(
                AdminAccessPolicy.hasAdminAccess(
                        SAMPLE_TOKEN, Set.of("view-clients", "query-clients", "query-users")));
    }

    @Test
    void deniegaSinRolesDeAdministracion() {
        assertFalse(AdminAccessPolicy.hasAdminAccess(SAMPLE_TOKEN, Set.of("osifirma-admin")));
    }
}
