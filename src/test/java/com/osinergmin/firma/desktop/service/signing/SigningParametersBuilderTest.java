package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.gob.osinergmin.firma.bean.ParametrosFirma;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SigningParametersBuilderTest {

    @TempDir Path tempDir;

    @Test
    void firmaInvisibleSinImagenUsaTipoFirmaCero() {
        SigningParam param = invisibleParam(false);
        ParametrosFirma built = build(param, Optional.empty(), false);

        assertEquals(0, built.getTipoFirma());
    }

    @Test
    void firmaSoloTextoSinImagenUsaTipoFirmaCuatro() {
        SigningParam defaults = SigningParam.defaults();
        SigningParam param =
                new SigningParam(
                        defaults.signatureFormat(),
                        defaults.originPath(),
                        defaults.destinationPath(),
                        defaults.fileName(),
                        4,
                        false,
                        defaults.stampTextSize(),
                        defaults.stampWordWrap(),
                        defaults.signatureTextTemplate(),
                        defaults.positioningMode(),
                        defaults.signatureTagName(),
                        defaults.stampPage(),
                        defaults.positionX(),
                        defaults.positionY(),
                        defaults.signatureLevel(),
                        defaults.webTsa(),
                        defaults.userTsa(),
                        defaults.passwordTsa(),
                        defaults.signatureReason());

        ParametrosFirma built = build(param, Optional.empty(), false);

        assertEquals(4, built.getTipoFirma());
    }

    private static SigningParam invisibleParam(boolean applyImage) {
        SigningParam defaults = SigningParam.defaults();
        return new SigningParam(
                defaults.signatureFormat(),
                defaults.originPath(),
                defaults.destinationPath(),
                defaults.fileName(),
                0,
                applyImage,
                defaults.stampTextSize(),
                defaults.stampWordWrap(),
                defaults.signatureTextTemplate(),
                defaults.positioningMode(),
                defaults.signatureTagName(),
                defaults.stampPage(),
                defaults.positionX(),
                defaults.positionY(),
                defaults.signatureLevel(),
                defaults.webTsa(),
                defaults.userTsa(),
                defaults.passwordTsa(),
                defaults.signatureReason());
    }

    private ParametrosFirma build(
            SigningParam param, Optional<Path> stampImage, boolean allowDragPlacement) {
        Path input = tempDir.resolve("input.pdf");
        Path output = tempDir.resolve("output.pdf");
        SigningWorkPaths paths = new SigningWorkPaths(input, output, false, false);
        return new SigningParametersBuilder()
                .build(paths, "alias-test", param, stampImage, Optional.empty(), allowDragPlacement);
    }
}
