package com.osinergmin.firma.desktop.integration;

import com.osinergmin.firma.desktop.config.ClientConfiguration;
import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class DocumentPathResolver {

    private DocumentPathResolver() {
    }

    public static Optional<Path> resolve(InvokeConfig config) {
        if (config.hasInlineDocument() || config.usesDocumentUrl()) {
            return Optional.empty();
        }
        String origen = config.getOrigenDocumento().toLowerCase();
        if ("local".equals(origen)) {
            String localPath = firstNonBlank(config.signingParam().originPath(), config.getRutaLocal());
            return resolveLocalPath(localPath);
        }
        if ("alfresco".equals(origen) && config.isUsarAlfresco()) {
            System.err.println("[DocumentPathResolver] origen alfresco aun no implementado; id="
                    + config.getIdDocumentoAlfresco());
            return Optional.empty();
        }
        if ("url".equals(origen) && !config.getDocumentoUrl().isBlank()) {
            System.err.println("[DocumentPathResolver] origen url aun no implementado; url="
                    + config.getDocumentoUrl());
            return Optional.empty();
        }
        return resolveLocalPath(firstNonBlank(config.signingParam().originPath(), config.getRutaLocal()));
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return "";
    }

    private static Optional<Path> resolveLocalPath(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return Optional.empty();
        }
        Path p = Path.of(ruta.trim());
        if (Files.isRegularFile(p)) {
            return Optional.of(p);
        }
        if (!p.isAbsolute()) {
            Path fromWorkDir = Path.of(System.getProperty("user.dir", ".")).resolve(p);
            if (Files.isRegularFile(fromWorkDir)) {
                return Optional.of(fromWorkDir);
            }
        }
        Optional<Path> base = ClientConfiguration.getDocumentsDirectory();
        if (base.isPresent()) {
            Path underBase = base.get().resolve(p.getFileName().toString());
            if (Files.isRegularFile(underBase)) {
                return Optional.of(underBase);
            }
        }
        return Optional.empty();
    }
}
