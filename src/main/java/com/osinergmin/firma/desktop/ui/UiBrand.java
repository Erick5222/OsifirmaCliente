package com.osinergmin.firma.desktop.ui;

import com.osinergmin.firma.desktop.App;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Objects;

/** Recursos de marca institucional (logo, icono de ventana). */
public final class UiBrand {

    /** Logo con franja amarilla — fondos oscuros (barra de titulo, icono de ventana). */
    public static final String LOGO_PATH = "images/osinergmin-logo.png";

    /** Logo vertical sobre blanco — tarjeta de login, sidebar, modales claros. */
    public static final String LOGO_ON_LIGHT_PATH = "images/osinergmin-logo-principal.png";

    private UiBrand() {
    }

    public static Image loadLogo() {
        return load(LOGO_PATH);
    }

    public static Image loadLogoOnLightBackground() {
        return load(LOGO_ON_LIGHT_PATH);
    }

    private static Image load(String resource) {
        var url = Objects.requireNonNull(App.class.getResource(resource), resource);
        return new Image(url.toExternalForm(), true);
    }

    public static void applyStageIcon(Stage stage) {
        if (stage == null) {
            return;
        }
        try (InputStream in = App.class.getResourceAsStream(LOGO_PATH)) {
            if (in != null) {
                stage.getIcons().setAll(new Image(in));
            }
        } catch (Exception ignored) {
            // Icono opcional; la app funciona sin él.
        }
    }
}
