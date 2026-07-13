package com.osinergmin.firma.desktop.ui;

import com.osinergmin.firma.desktop.App;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.util.Objects;

/** Carga Poppins desde classpath para toda la aplicación JavaFX. */
public final class UiFonts {

    public static final String FAMILY = "Poppins";

    private static volatile boolean loaded;

    private UiFonts() {
    }

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (UiFonts.class) {
            if (loaded) {
                return;
            }
            load("fonts/Poppins-Regular.ttf", 13);
            load("fonts/Poppins-SemiBold.ttf", 13);
            load("fonts/Poppins-Bold.ttf", 13);
            loaded = true;
        }
    }

    private static void load(String resource, double size) {
        try (InputStream in = Objects.requireNonNull(
                App.class.getResourceAsStream(resource),
                resource)) {
            Font.loadFont(in, size);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar la fuente " + resource, e);
        }
    }
}
