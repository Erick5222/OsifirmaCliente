package com.osinergmin.firma.desktop.integration.invoke;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalInvokeParamsTest {

    @Test
    void protocolUrlPopulatesAlfrescoInvocation() {
        String url =
                "osinergmin-firmador://firmar?"
                        + "idSolicitud=CP-01-ALF-ARRASTRE"
                        + "&origenDocumento=alfresco"
                        + "&usarAlfresco=true"
                        + "&nodeIdAlfresco=21141947-3136-4717-9419-473136d717db"
                        + "&documentVersion=1";

        ExternalInvokeParams params =
                ExternalInvokeParams.normalize(List.of(url), Map.of());

        InvokeConfig config = params.toInvokeConfig();

        assertEquals("alfresco", config.getOrigenDocumento());
        assertEquals("21141947-3136-4717-9419-473136d717db", config.getIdDocumentoAlfresco());
        assertTrue(config.isUsarAlfresco());
        assertTrue(config.usesAlfrescoDocument());
        assertEquals(1, config.getDocumentVersion().orElse(-1));
    }

    @Test
    void protocolUrlIncludesParamBlockFromBrowser() {
        String paramJson =
                "{\"signatureFormat\":1,\"positioningMode\":\"TA\",\"signatureTagName\":\"GYAURI\"}";
        String paramB64 = Base64.getEncoder().encodeToString(paramJson.getBytes(StandardCharsets.UTF_8));
        String url =
                "osinergmin-firmador://firmar?origenDocumento=alfresco&nodeIdAlfresco=abc&paramB64="
                        + paramB64;

        InvokeConfig config =
                ExternalInvokeParams.normalize(List.of(url), Map.of()).toInvokeConfig();

        assertEquals(1, config.signingParam().signatureFormat());
        assertEquals("GYAURI", config.signingParam().effectiveTagMarker());
        assertEquals("TA", config.signingParam().positioningMode());
        assertTrue(config.signingParam().usesTagPlacement());
    }
}
