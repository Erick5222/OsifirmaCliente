package com.osinergmin.firma.desktop.integration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/** Configuración ({@code config/client.properties}) y anulaciones JVM / entorno. */
public final class ClientConfiguration {

    private static final String RESOURCE = "/com/osinergmin/firma/desktop/config/client.properties";

    private static volatile Properties loaded;

    private ClientConfiguration() {
    }

    private static Properties properties() {
        if (loaded != null) {
            return loaded;
        }
        synchronized (ClientConfiguration.class) {
            if (loaded != null) {
                return loaded;
            }
            Properties p = new Properties();
            try (InputStream in = ClientConfiguration.class.getResourceAsStream(RESOURCE)) {
                if (in != null) {
                    p.load(in);
                }
            } catch (IOException ignored) {
            }
            loaded = p;
            return loaded;
        }
    }

    public static Optional<Path> getDocumentsDirectory() {
        String sys = System.getProperty("firma.documents.dir");
        if (sys != null && !sys.isBlank()) {
            return Optional.of(Path.of(sys.trim()));
        }
        String env = System.getenv("FIRMA_DOCUMENTS_DIR");
        if (env != null && !env.isBlank()) {
            return Optional.of(Path.of(env.trim()));
        }
        String v = properties().getProperty("documents.directory", "").trim();
        if (v.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Path.of(v));
    }

    public static boolean isFallbackToSampleWhenMissing() {
        String sys = System.getProperty("firma.pdf.fallbackSample");
        if (sys != null && !sys.isBlank()) {
            return Boolean.parseBoolean(sys.trim());
        }
        return Boolean.parseBoolean(properties().getProperty("viewer.fallbackToSampleWhenMissing", "true"));
    }
}
