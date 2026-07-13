package com.osinergmin.firma.desktop.service.document;

import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Resuelve el nodeId Alfresco (carpeta) donde subir el PDF firmado via POST /children. */
public final class AlfrescoDestinationResolver {

    private static final Pattern UUID =
            Pattern.compile(
                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private AlfrescoDestinationResolver() {}

    /**
     * La subida Alfresco es siempre a una carpeta ({@code destinationPath} UUID).
     * {@code documentVersion} solo aplica a la descarga del documento, no al nodeId de subida.
     */
    public static Optional<String> resolveUploadNodeId(InvokeConfig invokeConfig) {
        if (invokeConfig == null) {
            return Optional.empty();
        }
        Optional<String> folder = resolveFolderNodeId(invokeConfig.signingParam().destinationPath());
        if (folder.isPresent()) {
            return folder;
        }
        if (invokeConfig.usesAlfrescoDocument()) {
            return Optional.of(invokeConfig.getIdDocumentoAlfresco());
        }
        return Optional.empty();
    }

    public static boolean shouldUploadAfterSign(InvokeConfig invokeConfig) {
        return resolveUploadNodeId(invokeConfig).isPresent();
    }

    private static Optional<String> resolveFolderNodeId(String destinationPath) {
        if (destinationPath == null || destinationPath.isBlank()) {
            return Optional.empty();
        }
        String trimmed = destinationPath.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("alfresco:")) {
            String nodeId = trimmed.substring("alfresco:".length()).trim();
            return isUuid(nodeId) ? Optional.of(nodeId) : Optional.empty();
        }
        return isUuid(trimmed) ? Optional.of(trimmed) : Optional.empty();
    }

    private static boolean isUuid(String value) {
        return value != null && UUID.matcher(value.trim()).matches();
    }
}
