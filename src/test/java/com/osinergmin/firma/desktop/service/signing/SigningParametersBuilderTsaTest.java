package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SigningParametersBuilderTsaTest {

    private final SigningParametersBuilder builder = new SigningParametersBuilder();

    @Test
    void nivelBasicoNoExigeTsa() {
        SigningParam param =
                new SigningParam(
                        1,
                        "",
                        "",
                        "doc.pdf",
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
                        "test");
        assertTrue(builder.validateTsaForSignatureLevel(param).isEmpty());
    }

    @Test
    void nivelTSinTsaUrlFallaValidacion() {
        SigningParam param =
                new SigningParam(
                        1,
                        "",
                        "",
                        "doc.pdf",
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
                        "T",
                        "",
                        "",
                        "",
                        "test");
        Optional<String> error = builder.validateTsaForSignatureLevel(param);
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("TSA"));
    }
}
