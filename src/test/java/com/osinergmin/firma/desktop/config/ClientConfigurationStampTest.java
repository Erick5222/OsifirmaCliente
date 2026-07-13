package com.osinergmin.firma.desktop.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientConfigurationStampTest {

    @Test
    void resolveSigningImageFile_usesBundledStampWhenPathNotConfigured() {
        Path stamp = ClientConfiguration.resolveSigningImageFile().orElse(null);
        assertTrue(stamp != null && Files.isRegularFile(stamp), "sello empaquetado disponible");
    }
}
