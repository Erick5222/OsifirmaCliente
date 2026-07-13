package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.config.ClientConfiguration;
import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/** Archivos temporales de sello/imagen derivados de la parametria de invocacion. */
public final class SigningInvokeResources {

    private SigningInvokeResources() {}

    public static Optional<Path> resolveStampImagePath(InvokeConfig invokeConfig) throws IOException {
        if (invokeConfig == null || !invokeConfig.signingParam().applyImage()) {
            return Optional.empty();
        }
        Optional<byte[]> stampBytes = invokeConfig.stampBytes();
        if (stampBytes.isPresent()) {
            Path workDir = resolveWorkDirectory();
            Files.createDirectories(workDir);
            Path stampPath = workDir.resolve("stamp-" + UUID.randomUUID() + ".png");
            Files.write(stampPath, stampBytes.get());
            return Optional.of(stampPath);
        }
        return ClientConfiguration.resolveSigningImageFile();
    }

    private static Path resolveWorkDirectory() {
        return ClientConfiguration.getDocumentsDirectory()
                .map(path -> path.resolve(".firma-work"))
                .orElseGet(
                        () ->
                                Path.of(System.getProperty("java.io.tmpdir", "."))
                                        .resolve("osinergmin-firmador"));
    }
}
