package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SignaturePlacementConversionTest {

    private static final float PAGE_W = 595.32f;
    private static final float PAGE_H = 841.92f;

    @Test
    void posYCreceHaciaAbajoComoPresetsDelMotor() {
        SignaturePlacement arriba = placementAt(0.1);
        SignaturePlacement abajo = placementAt(0.85);

        int posYArriba = convertPosYViaBuilder(arriba, 1);
        int posYAbajo = convertPosYViaBuilder(abajo, 1);

        assertTrue(posYArriba < posYAbajo, "Abajo en visor debe dar posy mayor que arriba");
        assertTrue(posYArriba < 150, "Cerca del borde superior");
        assertTrue(posYAbajo > 500, "Cerca del borde inferior");
    }

    private static SignaturePlacement placementAt(double verticalRatio) {
        double overlayH = 800;
        double overlayY = verticalRatio * overlayH;
        return new SignaturePlacement(
                1,
                100,
                overlayY,
                600,
                overlayH,
                160,
                72,
                PAGE_W,
                PAGE_H);
    }

    private static int convertPosYViaBuilder(SignaturePlacement sp, int style) {
        SigningParametersBuilder builder = new SigningParametersBuilder();
        String log = builder.describeForLog(SigningParam.defaults(), Optional.of(sp), true);
        int idx = log.indexOf("posy=");
        return Integer.parseInt(log.substring(idx + 5, log.indexOf(" (visor", idx)));
    }
}
