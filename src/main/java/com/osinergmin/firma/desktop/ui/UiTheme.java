package com.osinergmin.firma.desktop.ui;

import com.osinergmin.firma.desktop.App;
import javafx.scene.Scene;

import java.util.Objects;

/** Aplica la hoja de estilos institucional a una {@link Scene}. */
public final class UiTheme {

    private UiTheme() {
    }

    public static void apply(Scene scene) {
        UiFonts.ensureLoaded();
        var css = Objects.requireNonNull(
                App.class.getResource("css/osinergmin-theme.css"),
                "css/osinergmin-theme.css");
        scene.getStylesheets().clear();
        scene.getStylesheets().add(css.toExternalForm());
    }
}
