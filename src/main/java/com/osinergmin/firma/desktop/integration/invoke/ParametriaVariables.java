package com.osinergmin.firma.desktop.integration.invoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Resuelve placeholders de parametria.json y casos de prueba.
 *
 * <p>Placeholders soportados: {@code ${JWT_TOKEN}}, {@code ${NODE_ID_ALFRESCO}},
 * {@code ${NODE_ID_CARPETA_ALFRESCO}}, {@code ${NODE_ID_ALFRESCO_XML}}, {@code ${NODE_ID_ALFRESCO_DOCX}},
 * {@code ${DOCUMENT_BASE64}}, {@code ${REFRESH_TOKEN}},
 * {@code ${CALLBACK_URL}} y equivalentes legacy {@code <JWT_TOKEN>}, etc.
 */
final class ParametriaVariables {

    static final String JWT_TOKEN = "${JWT_TOKEN}";
    static final String NODE_ID_ALFRESCO = "${NODE_ID_ALFRESCO}";
    static final String NODE_ID_CARPETA_ALFRESCO = "${NODE_ID_CARPETA_ALFRESCO}";
    static final String DOCUMENT_BASE64 = "${DOCUMENT_BASE64}";
    static final String REFRESH_TOKEN = "${REFRESH_TOKEN}";
    static final String CALLBACK_URL = "${CALLBACK_URL}";

    private ParametriaVariables() {}

    static String resolve(String value, Path parametriaFile) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (matchesPlaceholder(trimmed, "JWT_TOKEN")) {
            return firstNonBlank(env("FIRMA_JWT_TOKEN"), systemProperty("firma.jwt.token"));
        }
        if (matchesPlaceholder(trimmed, "NODE_ID_ALFRESCO")) {
            return firstNonBlank(env("FIRMA_NODE_ID_ALFRESCO"), systemProperty("firma.node.id.alfresco"));
        }
        if (matchesPlaceholder(trimmed, "NODE_ID_CARPETA_ALFRESCO")) {
            return firstNonBlank(
                    env("FIRMA_NODE_ID_CARPETA_ALFRESCO"), systemProperty("firma.node.id.carpeta.alfresco"));
        }
        if (matchesPlaceholder(trimmed, "NODE_ID_ALFRESCO_XML")) {
            return firstNonBlank(
                    env("FIRMA_NODE_ID_ALFRESCO_XML"),
                    systemProperty("firma.node.id.alfresco.xml"),
                    env("FIRMA_NODE_ID_ALFRESCO"),
                    systemProperty("firma.node.id.alfresco"));
        }
        if (matchesPlaceholder(trimmed, "NODE_ID_ALFRESCO_DOCX")) {
            return firstNonBlank(
                    env("FIRMA_NODE_ID_ALFRESCO_DOCX"),
                    systemProperty("firma.node.id.alfresco.docx"),
                    env("FIRMA_NODE_ID_ALFRESCO"),
                    systemProperty("firma.node.id.alfresco"));
        }
        if (matchesPlaceholder(trimmed, "REFRESH_TOKEN")) {
            return firstNonBlank(env("FIRMA_REFRESH_TOKEN"), systemProperty("firma.refresh.token"));
        }
        if (matchesPlaceholder(trimmed, "CALLBACK_URL")) {
            return firstNonBlank(env("FIRMA_CALLBACK_URL"), systemProperty("firma.callback.url"));
        }
        if (matchesPlaceholder(trimmed, "DOCUMENT_BASE64")) {
            return resolveDocumentBase64(parametriaFile);
        }
        return value;
    }

    static boolean isUnresolvedPlaceholder(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        if (matchesPlaceholder(
                trimmed,
                "JWT_TOKEN",
                "NODE_ID_ALFRESCO",
                "NODE_ID_CARPETA_ALFRESCO",
                "NODE_ID_ALFRESCO_XML",
                "NODE_ID_ALFRESCO_DOCX",
                "REFRESH_TOKEN",
                "CALLBACK_URL")) {
            return true;
        }
        return matchesPlaceholder(trimmed, "DOCUMENT_BASE64")
                && resolveDocumentBase64(null).isBlank();
    }

    private static String resolveDocumentBase64(Path parametriaFile) {
        String fromEnv = env("FIRMA_DOCUMENT_BASE64");
        if (!fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromProperty = systemProperty("firma.document.base64");
        if (!fromProperty.isBlank()) {
            return fromProperty;
        }
        for (Path candidate : documentBase64Candidates(parametriaFile)) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                try {
                    return Files.readString(candidate, StandardCharsets.UTF_8).trim();
                } catch (IOException e) {
                    System.err.println("[ParametriaVariables] No se pudo leer " + candidate + ": " + e.getMessage());
                }
            }
        }
        return "";
    }

    private static Path[] documentBase64Candidates(Path parametriaFile) {
        if (parametriaFile == null) {
            return new Path[] {
                Path.of("parametria-casos", "vars", "document.base64"),
                Path.of("document.base64")
            };
        }
        Path parent = parametriaFile.getParent();
        Path grandParent = parent != null ? parent.getParent() : null;
        return new Path[] {
            parent != null ? parent.resolve("vars").resolve("document.base64") : null,
            parent != null ? parent.resolve("document.base64") : null,
            grandParent != null ? grandParent.resolve("parametria-casos").resolve("vars").resolve("document.base64") : null,
            Path.of("parametria-casos", "vars", "document.base64"),
            Path.of("document.base64")
        };
    }

    private static boolean matchesPlaceholder(String value, String... names) {
        for (String name : names) {
            String upper = name.toUpperCase(Locale.ROOT);
            if (value.equals("${" + upper + "}") || value.equals("<" + upper + ">")) {
                return true;
            }
            if ("DOCUMENT_BASE64".equals(upper)
                    && (value.equals("<REEMPLAZAR_CON_BASE64_DEL_PDF>")
                            || value.equals("<BASE64_DOCUMENTO>"))) {
                return true;
            }
        }
        return false;
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private static String systemProperty(String name) {
        String value = System.getProperty(name);
        return value == null ? "" : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
