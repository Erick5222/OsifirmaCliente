package com.osinergmin.firma.desktop.service.signing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Rutas de entrada/salida para firma; elimina temporales creados por el servicio. */
final class SigningWorkPaths {

    private final Path inputPath;
    private final Path outputPath;
    private final boolean deleteInputOnCleanup;
    private final boolean deleteOutputOnCleanup;

    SigningWorkPaths(
            Path inputPath,
            Path outputPath,
            boolean deleteInputOnCleanup,
            boolean deleteOutputOnCleanup) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.deleteInputOnCleanup = deleteInputOnCleanup;
        this.deleteOutputOnCleanup = deleteOutputOnCleanup;
    }

    Path inputPath() {
        return inputPath;
    }

    Path outputPath() {
        return outputPath;
    }

    void cleanupOnFailure() {
        deleteIfMarked(outputPath, deleteOutputOnCleanup);
        deleteIfMarked(inputPath, deleteInputOnCleanup);
    }

    void cleanupInputIfTemporary() {
        deleteIfMarked(inputPath, deleteInputOnCleanup);
    }

    void cleanupOutputIfTemporary() {
        deleteIfMarked(outputPath, deleteOutputOnCleanup);
    }

    void cleanupAllTemporary() {
        cleanupOutputIfTemporary();
        cleanupInputIfTemporary();
    }

    private static void deleteIfMarked(Path path, boolean marked) {
        if (!marked || path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
