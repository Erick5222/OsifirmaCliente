package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigningDocumentNamingTest {

    @Test
    void xadesRejectsNonXmlFileName() {
        SigningParam param = new SigningParam(
                2,
                "",
                "",
                "doc.pdf",
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
        var validation = SigningDocumentNaming.validateForSigning(param, "documento.pdf");
        assertFalse(validation.valid());
        assertTrue(validation.message().contains("XML"));
    }

    @Test
    void cadesSignedNameUsesP7sExtension() {
        SigningParam param = new SigningParam(
                3,
                "",
                "",
                "informe.pdf",
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
        assertEquals("informe[F].p7s", SigningDocumentNaming.buildSignedDisplayName("informe.pdf", param));
    }

    @Test
    void xadesSignedNameKeepsXmlExtension() {
        SigningParam param = new SigningParam(
                2,
                "",
                "",
                "dato.xml",
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
        assertEquals("dato[F].xml", SigningDocumentNaming.buildSignedDisplayName("dato.xml", param));
    }
}
