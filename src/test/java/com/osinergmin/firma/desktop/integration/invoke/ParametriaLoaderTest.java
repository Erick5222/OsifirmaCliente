package com.osinergmin.firma.desktop.integration.invoke;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParametriaLoaderTest {

    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));

    @AfterEach
    void clearProperties() {
        System.clearProperty("firma.jwt.token");
        System.clearProperty("firma.node.id.alfresco");
        System.clearProperty("firma.document.base64");
    }

    @Test
    void cargaParametriaMinimaAlfrescoConDefaults() {
        Optional<InvokeConfig> loaded =
                ParametriaLoader.load(PROJECT_ROOT.resolve("parametria-casos/CP-02-alfresco-posicion-fija.json"));
        assertTrue(loaded.isPresent(), "CP-02 debe cargar");

        InvokeConfig config = loaded.get();
        assertTrue(config.isUsarAlfresco());
        assertFalse(config.isFirmaArrastrar());
        assertTrue(config.isSinBandeja());
        assertEquals("alfresco", config.getOrigenDocumento());
        assertEquals(80, config.signingParam().positionY());
        assertEquals(1, config.signingParam().signatureFormat());
        assertTrue(config.signingParam().applyImage());
        assertTrue(config.usesAlfrescoVersioning());
        assertEquals(3, config.getDocumentVersion().orElse(0));
    }

    @Test
    void cargaDocumentVersionDesdeParamEnCp12() {
        Optional<InvokeConfig> loaded =
                ParametriaLoader.load(
                        PROJECT_ROOT.resolve("parametria-casos/CP-12-alfresco-version-en-param.json"));
        assertTrue(loaded.isPresent(), "CP-12 debe cargar");

        InvokeConfig config = loaded.get();
        assertTrue(config.usesAlfrescoVersioning());
        assertEquals(3, config.getDocumentVersion().orElse(0));
        assertEquals(4, config.targetDocumentVersion());
    }

    @Test
    void resuelvePlaceholdersDesdePropiedadesDelSistema() {
        System.setProperty("firma.jwt.token", "jwt-prueba");
        System.setProperty("firma.node.id.alfresco", "node-123");

        Optional<InvokeConfig> loaded =
                ParametriaLoader.load(PROJECT_ROOT.resolve("parametria-casos/CP-01-alfresco-arrastre.json"));
        assertTrue(loaded.isPresent());

        InvokeConfig config = loaded.get();
        assertEquals("jwt-prueba", config.getToken());
        assertEquals("node-123", config.getIdDocumentoAlfresco());
        assertTrue(config.isFirmaArrastrar());
    }

    @Test
    void documentVersionDefaultEs1ParaAlfresco() throws Exception {
        Path tempDir = Files.createTempDirectory("parametria-version-default");
        String json =
                """
                {
                  "invocacion": {
                    "origenDocumento": "alfresco",
                    "nodeIdAlfresco": "64199015-e83b-469a-9990-15e83b769a09"
                  }
                }
                """;
        Path jsonFile = tempDir.resolve("alfresco-sin-version.json");
        Files.writeString(jsonFile, json);

        InvokeConfig config = ParametriaLoader.load(jsonFile).orElseThrow();
        assertTrue(config.usesAlfrescoVersioning());
        assertEquals(1, config.getDocumentVersion().orElse(0));
        assertEquals(2, config.targetDocumentVersion());
    }

    @Test
    void resuelveDocumentBase64DesdeArchivoVars() throws Exception {
        Path tempDir = Files.createTempDirectory("parametria-loader-test");
        Path varsDir = tempDir.resolve("vars");
        Files.createDirectories(varsDir);

        byte[] miniPdf = "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);
        Files.writeString(varsDir.resolve("document.base64"), Base64.getEncoder().encodeToString(miniPdf));

        String json =
                """
                {
                  "invocacion": {
                    "idSolicitud": "TEST-BYTES",
                    "origenDocumento": "bytes"
                  },
                  "document": "${DOCUMENT_BASE64}"
                }
                """;
        Path jsonFile = tempDir.resolve("bytes.json");
        Files.writeString(jsonFile, json);

        Optional<InvokeConfig> loaded = ParametriaLoader.load(jsonFile);
        assertTrue(loaded.isPresent());

        InvokeConfig config = loaded.get();
        assertEquals("bytes", config.getOrigenDocumento());
        assertTrue(config.hasInlineDocument());
        assertEquals(miniPdf.length, config.documentBytes().orElseThrow().length);
    }

    @Test
    void cargaInterfazMinimaDesdeInvocacion() {
        Optional<InvokeConfig> loaded =
                ParametriaLoader.load(
                        PROJECT_ROOT.resolve("parametria-casos/CP-17-interfaz-minima.json"));
        assertTrue(loaded.isPresent(), "CP-17 debe cargar");

        InvokeConfig config = loaded.get();
        assertTrue(config.isInterfazMinima());
        assertTrue(config.isCerrarAlTerminar(), "interfazMinima implica cierre al terminar");
        assertTrue(config.shouldSkipDocumentViewer());
    }

    @Test
    void cargaParametriaOperativaDelProyecto() {
        Path parametria = PROJECT_ROOT.resolve("parametria.json");
        if (!Files.isRegularFile(parametria)) {
            return;
        }
        Optional<InvokeConfig> loaded = ParametriaLoader.load(parametria);
        assertTrue(loaded.isPresent(), "parametria.json debe ser valida");

        InvokeConfig config = loaded.get();
        assertFalse(config.getOrigenDocumento().isBlank());
        if ("url".equalsIgnoreCase(config.getOrigenDocumento())) {
            assertTrue(config.usesDocumentUrl());
            assertFalse(config.getDocumentoUrl().isBlank());
        }
    }
}
