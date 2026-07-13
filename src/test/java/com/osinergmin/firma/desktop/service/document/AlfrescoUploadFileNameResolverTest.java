package com.osinergmin.firma.desktop.service.document;

import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;
import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlfrescoUploadFileNameResolverTest {

    @Test
    void stripSignedSuffixNoRecortaUltimoDigitoDelNombre() {
        assertEquals(
                "informe_firmado_dat1.pdf",
                AlfrescoUploadFileNameResolver.stripSignedSuffix("informe_firmado_dat1[F].pdf"));
    }

    @Test
    void stripSignedSuffixSinSufijoDevuelveMismoNombre() {
        assertEquals("informe_firmado_dat1.pdf", AlfrescoUploadFileNameResolver.stripSignedSuffix("informe_firmado_dat1.pdf"));
    }

    @Test
    void resolvePriorizaFileNameDeParametria() {
        SigningParam param =
                new SigningParam(
                        3,
                        "",
                        "folder-uuid",
                        "pruebas2Firmado.p7s",
                        4,
                        false,
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
                        "test");
        InvokeConfig config =
                new InvokeConfig(
                        "SOL",
                        "",
                        "",
                        "",
                        "alfresco",
                        "",
                        "",
                        "node",
                        "application/pdf",
                        true,
                        true,
                        true,
                        false,
                        "soloFirma",
                        false,
                        false,
                        false,
                        "",
                        param,
                        1,
                        null,
                        null);
        PdfViewerConfig signed =
                PdfViewerConfig.fromInMemory(new byte[] {1}, "pruebas2Firmado[F].p7s");
        assertEquals("pruebas2Firmado.p7s", AlfrescoUploadFileNameResolver.resolve(config, signed));
    }

    @Test
    void resolveSinParamUsaNombreFirmadoEnSesion() {
        PdfViewerConfig signed = PdfViewerConfig.fromInMemory(new byte[] {1}, "pruebas2Firmado[F].p7s");
        assertEquals("pruebas2Firmado[F].p7s", AlfrescoUploadFileNameResolver.resolve(null, signed));
    }
}
