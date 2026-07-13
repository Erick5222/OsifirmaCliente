package com.osinergmin.firma.desktop.service.signing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;
import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigningCallbackClientTest {

    @Test
    void buildPayload_includesSignedContentBase64OnSuccess() {
        byte[] signed = "%PDF-1.4 firmado".getBytes(StandardCharsets.UTF_8);
        PdfViewerConfig document = PdfViewerConfig.fromInMemoryPdf(signed, "informe_firmado.pdf");
        InvokeConfig config =
                new InvokeConfig(
                        "CP-15",
                        "",
                        "",
                        "http://localhost:8080/api/firma/callback",
                        "alfresco",
                        "",
                        "",
                        "node",
                        "application/pdf",
                        true,
                        true,
                        true,
                        false,
                        "completo",
                        false,
                        false,
                        false,
                        "",
                        SigningParam.defaults(),
                        1,
                        null,
                        null);

        String json =
                SigningCallbackClient.buildPayload(
                        config, true, null, "original".getBytes(StandardCharsets.UTF_8), document, null);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("SUCCESS", root.get("status").getAsString());
        JsonObject doc = root.getAsJsonObject("document");
        assertEquals("informe_firmado.pdf", doc.get("fileName").getAsString());
        assertEquals(signed.length, doc.get("fileSize").getAsInt());
        String b64 = doc.get("signedContentBase64").getAsString();
        assertFalse(b64.isBlank());
        assertArrayEquals(signed, Base64.getDecoder().decode(b64));
    }

    @Test
    void buildPayload_errorDoesNotIncludeDocumentBytes() {
        InvokeConfig config =
                new InvokeConfig(
                        "SOL-ERR",
                        "",
                        "",
                        "http://localhost:8080/api/firma/callback",
                        "local",
                        "",
                        "",
                        "",
                        "",
                        false,
                        false,
                        true,
                        false,
                        "completo",
                        true,
                        false,
                        false,
                        "",
                        SigningParam.defaults(),
                        null,
                        null,
                        null);

        String json = SigningCallbackClient.buildPayload(config, false, "Fallo de prueba", new byte[0], null, null);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("ERROR", root.get("status").getAsString());
        assertEquals("Fallo de prueba", root.get("errorMessage").getAsString());
        assertTrue(root.get("document").isJsonNull());
    }

    private static void assertArrayEquals(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }
}
