package com.osinergmin.firma.desktop.service.verification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationResultTest {

    @Test
    void failureIndicaEstadoSinFirmas() {
        VerificationResult result = VerificationResult.failure("Error de prueba");

        assertFalse(result.success());
        assertTrue(result.estadoLabel().contains("Sin firmas"));
        assertTrue(result.technicalDetailText().contains("Error de prueba"));
    }
}
