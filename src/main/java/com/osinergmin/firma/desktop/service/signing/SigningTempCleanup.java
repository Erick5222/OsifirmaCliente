package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.config.ClientConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Elimina temporales del firmador al cerrar la aplicacion. */
public final class SigningTempCleanup {

    private SigningTempCleanup() {}

    public static void purgeWorkDirectory() {
        deleteDirectoryContents(resolveWorkDirectory());
    }

    private static Path resolveWorkDirectory() {
        return ClientConfiguration.getDocumentsDirectory()
                .map(path -> path.resolve(".firma-work"))
                .orElseGet(
                        () ->
                                Path.of(System.getProperty("java.io.tmpdir", "."))
                                        .resolve("osinergmin-firmador"));
    }

    private static void deleteDirectoryContents(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> entries = Files.list(directory)) {
            entries.sorted(Comparator.reverseOrder())
                    .forEach(
                            path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                    // best effort
                                }
                            });
        } catch (IOException ignored) {
            // best effort
        }
        try {
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
