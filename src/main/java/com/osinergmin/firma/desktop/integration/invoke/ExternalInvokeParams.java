package com.osinergmin.firma.desktop.integration.invoke;

import javafx.application.Application;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ExternalInvokeParams {

    private final List<String> unnamed;
    private final Map<String, String> named;

    private ExternalInvokeParams(List<String> unnamed, Map<String, String> named) {
        this.unnamed = List.copyOf(unnamed);
        this.named = Map.copyOf(named);
    }

    public static ExternalInvokeParams fromApplication(Application application) {
        var p = application.getParameters();
        return normalize(p.getUnnamed(), p.getNamed());
    }

    static ExternalInvokeParams normalize(List<String> rawUnnamed, Map<String, String> rawNamed) {
        List<String> unnamed = new ArrayList<>(rawUnnamed);
        Map<String, String> named = new LinkedHashMap<>(rawNamed);

        Optional<String> protocolUrl = unnamed.stream()
                .filter(ExternalInvokeParams::looksLikeCustomProtocol)
                .findFirst();
        if (protocolUrl.isPresent()) {
            mergeProtocolQuery(protocolUrl.get(), named);
            unnamed.remove(protocolUrl.get());
        }

        return new ExternalInvokeParams(unnamed, named);
    }

    public InvokeConfig toInvokeConfig() {
        String file = named.getOrDefault("file", "");
        if (file.isBlank()) {
            file = firstExistingFileArgument().map(Path::toString).orElse("");
        }
        String origen = named.getOrDefault("origenDocumento", "local");
        String alfrescoNode = firstNonBlank(named.get("idDocumentoAlfresco"), named.get("nodeIdAlfresco"));
        boolean alfresco = flag("usarAlfresco") || "alfresco".equalsIgnoreCase(origen);
        Integer documentVersion = parseInteger(named.get("documentVersion"));
        if (documentVersion == null && alfresco && alfrescoNode != null && !alfrescoNode.isBlank()) {
            documentVersion = 1;
        }
        SigningParam signingParam =
                ParametriaLoader.parseParamFromProtocol(named).orElseGet(SigningParam::defaults);
        return new InvokeConfig(
                named.getOrDefault("idSolicitud", ""),
                named.getOrDefault("token", ""),
                named.getOrDefault("refresh_token", ""),
                named.getOrDefault("callbackUrl", ""),
                origen,
                file,
                named.getOrDefault("documentoUrl", ""),
                alfrescoNode != null ? alfrescoNode : "",
                named.getOrDefault("contentType", ""),
                alfresco,
                flag("sinVisor"),
                flag("sinBandeja"),
                flag("abrirVisorPrimero"),
                named.getOrDefault("modo", ""),
                dragFlag("FirmaArrastrar", "firmaArrastrar"),
                flag("cerrarAlTerminar"),
                flag("interfazMinima"),
                named.getOrDefault("credential", ""),
                signingParam,
                documentVersion,
                null,
                null);
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Optional<Path> firstExistingFileArgument() {
        return unnamed.stream()
                .map(Path::of)
                .filter(Files::isRegularFile)
                .findFirst();
    }

    private boolean flag(String key) {
        return "true".equalsIgnoreCase(named.getOrDefault(key, "").trim());
    }

    private boolean dragFlag(String... keys) {
        for (String key : keys) {
            String value = named.get(key);
            if (value != null && !value.isBlank()) {
                return !"false".equalsIgnoreCase(value.trim());
            }
        }
        return true;
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

    private static boolean looksLikeCustomProtocol(String s) {
        if (s == null) {
            return false;
        }
        String t = s.trim().toLowerCase();
        return t.startsWith("osinergmin-firmador:") || t.startsWith("firmador:");
    }

    private static void mergeProtocolQuery(String url, Map<String, String> named) {
        try {
            URI uri = URI.create(url.trim());
            String query = uri.getRawQuery();
            if (query == null || query.isBlank()) {
                return;
            }
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                    named.putIfAbsent(key, value);
                } else if (!pair.isBlank()) {
                    named.putIfAbsent(URLDecoder.decode(pair, StandardCharsets.UTF_8), "true");
                }
            }
        } catch (IllegalArgumentException ignored) {
            // URL mal formada
        }
    }
}
