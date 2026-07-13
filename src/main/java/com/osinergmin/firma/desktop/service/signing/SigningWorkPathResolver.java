package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.config.ClientConfiguration;
import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Rutas de trabajo de entrada/salida para el motor de firma. */
final class SigningWorkPathResolver {

    private SigningWorkPathResolver() {}

    static SigningWorkPaths prepare(PdfViewerConfig source, SigningParam param) throws IOException {
        String inputExt = SigningDocumentNaming.inputExtensionFor(param, source.displayFileName());
        if (source.usesLocalFile()) {
            Path input = Path.of(source.fileSystemPath()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(input)) {
                throw new IOException("No se encontro el archivo: " + input);
            }
            if (param == null || !param.usesTagPlacement()) {
                Path output = resolveOutputPath(input, param);
                return new SigningWorkPaths(input, output, false, false);
            }
        }

        return materializeToWorkDir(source, param, inputExt);
    }

    private static SigningWorkPaths materializeToWorkDir(
            PdfViewerConfig source, SigningParam param, String inputExt) throws IOException {
        Path workDir = resolveWorkDirectory();
        Files.createDirectories(workDir);
        String token = UUID.randomUUID().toString();
        Path input = workDir.resolve("entrada-" + token + inputExt);
        Path output = workDir.resolve("salida-" + token + SigningDocumentNaming.outputExtensionFor(param));
        Files.write(input, source.readDocumentBytes());
        return new SigningWorkPaths(input, output, true, true);
    }

    private static Path resolveOutputPath(Path input, SigningParam param) {
        if (param.destinationPath() != null && !param.destinationPath().isBlank()) {
            String destination = param.destinationPath().trim();
            if (!destination.startsWith("alfresco:")) {
                return Path.of(destination).toAbsolutePath().normalize();
            }
        }
        return SigningDocumentNaming.buildSignedSiblingPath(input, param);
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
