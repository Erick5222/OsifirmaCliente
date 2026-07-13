package com.osinergmin.firma.desktop.service.document;

import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;
import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlfrescoDestinationResolverTest {

    private static final String NODE = "64199015-e83b-469a-9990-15e83b769a09";
    private static final String OTHER = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Test
    void usaMismoNodeIdCuandoOrigenEsAlfresco() {
        InvokeConfig config = alfrescoConfig("", NODE);
        assertEquals(NODE, AlfrescoDestinationResolver.resolveUploadNodeId(config).orElseThrow());
        assertTrue(AlfrescoDestinationResolver.shouldUploadAfterSign(config));
    }

    @Test
    void usaDestinationPathComoUuid() {
        InvokeConfig config = localConfig(OTHER);
        assertEquals(OTHER, AlfrescoDestinationResolver.resolveUploadNodeId(config).orElseThrow());
    }

    @Test
    void usaPrefijoAlfrescoEnDestinationPath() {
        InvokeConfig config = localConfig("alfresco:" + OTHER);
        assertEquals(OTHER, AlfrescoDestinationResolver.resolveUploadNodeId(config).orElseThrow());
    }

    @Test
    void noSubeSiDestinationPathEsRutaLocal() {
        InvokeConfig config = localConfig("D:/salida/documento[F].pdf");
        assertFalse(AlfrescoDestinationResolver.shouldUploadAfterSign(config));
    }

    @Test
    void conVersionUsaCarpetaDestinoParaSubida() {
        InvokeConfig config = alfrescoConfig(OTHER, NODE, 3);
        assertEquals(OTHER, AlfrescoDestinationResolver.resolveUploadNodeId(config).orElseThrow());
    }

    private static InvokeConfig alfrescoConfig(String destinationPath, String nodeId) {
        return alfrescoConfig(destinationPath, nodeId, null);
    }

    private static InvokeConfig alfrescoConfig(String destinationPath, String nodeId, Integer documentVersion) {
        SigningParam param =
                new SigningParam(
                        1,
                        "",
                        destinationPath,
                        "documento.pdf",
                        1,
                        true,
                        13,
                        37,
                        "",
                        "",
                        "",
                        1,
                        10,
                        400,
                        "B",
                        "",
                        "",
                        "",
                        "Firma digital OSINERGMIN");
        return new InvokeConfig(
                "SOL-1",
                "token",
                "",
                "",
                "alfresco",
                "",
                "",
                nodeId,
                "application/pdf",
                true,
                false,
                true,
                false,
                "completo",
                false,
                false,
                false,
                "",
                param,
                documentVersion,
                null,
                null);
    }

    private static InvokeConfig localConfig(String destinationPath) {
        SigningParam param =
                new SigningParam(
                        1,
                        "",
                        destinationPath,
                        "documento.pdf",
                        1,
                        true,
                        13,
                        37,
                        "",
                        "",
                        "",
                        1,
                        10,
                        400,
                        "B",
                        "",
                        "",
                        "",
                        "Firma digital OSINERGMIN");
        return new InvokeConfig(
                "SOL-1",
                "",
                "",
                "",
                "local",
                "documento.pdf",
                "",
                "",
                "application/pdf",
                false,
                false,
                true,
                false,
                "completo",
                true,
                false,
                false,
                "",
                param,
                null,
                null,
                null);
    }
}
