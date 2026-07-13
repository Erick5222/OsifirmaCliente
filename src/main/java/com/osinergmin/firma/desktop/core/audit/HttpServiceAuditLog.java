package com.osinergmin.firma.desktop.core.audit;

import com.osinergmin.firma.desktop.config.ClientConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/** Registro en archivo de consumos HTTP (request/response) para diagnostico. */
public final class HttpServiceAuditLog {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Pattern BASE64_JSON =
            Pattern.compile("\"base64Content\"\\s*:\\s*\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEARER_HEADER =
            Pattern.compile("Bearer\\s+\\S+", Pattern.CASE_INSENSITIVE);
    private static final Object LOCK = new Object();

    private HttpServiceAuditLog() {}

    public static void recordSuccess(
            String component,
            String method,
            String url,
            String requestSummary,
            int httpStatus,
            String responseSummary) {
        if (!ClientConfiguration.isAuditLogEnabled()) {
            return;
        }
        write(buildEntry(component, method, url, requestSummary, httpStatus, responseSummary, null, "OK"));
    }

    public static void recordFailure(
            String component,
            String method,
            String url,
            String requestBody,
            int httpStatus,
            String responseBody,
            String errorMessage) {
        if (!ClientConfiguration.isAuditLogEnabled()) {
            return;
        }
        write(
                buildEntry(
                        component,
                        method,
                        url,
                        sanitizeRequest(requestBody),
                        httpStatus,
                        truncate(sanitizeResponse(responseBody), 8000),
                        errorMessage,
                        "ERROR"));
    }

    public static void recordFailure(
            String component,
            String method,
            String url,
            String requestBody,
            String errorMessage) {
        recordFailure(component, method, url, requestBody, 0, "", errorMessage);
    }

    public static Path getLogFilePath() {
        return ClientConfiguration.getAuditLogFilePath();
    }

    private static String buildEntry(
            String component,
            String method,
            String url,
            String requestSummary,
            int httpStatus,
            String responseSummary,
            String errorMessage,
            String outcome) {
        StringBuilder sb = new StringBuilder(512);
        sb.append('[').append(TS.format(LocalDateTime.now())).append("] ");
        sb.append(outcome).append(' ').append(nullToDash(component));
        sb.append(" | ").append(nullToDash(method)).append(' ').append(nullToDash(url));
        if (httpStatus > 0) {
            sb.append(" | HTTP ").append(httpStatus);
        }
        sb.append(System.lineSeparator());
        sb.append("  request: ").append(truncate(requestSummary, 4000)).append(System.lineSeparator());
        if (responseSummary != null && !responseSummary.isBlank()) {
            sb.append("  response: ").append(truncate(responseSummary, 8000)).append(System.lineSeparator());
        }
        if (errorMessage != null && !errorMessage.isBlank()) {
            sb.append("  error: ").append(truncate(errorMessage, 2000)).append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    private static void write(String entry) {
        Path logFile = getLogFilePath();
        synchronized (LOCK) {
            try {
                Path parent = logFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(
                        logFile,
                        entry,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (IOException ex) {
                System.err.println("[HttpServiceAuditLog] No se pudo escribir " + logFile + ": " + ex.getMessage());
            }
        }
    }

    private static String sanitizeRequest(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = BEARER_HEADER.matcher(value).replaceAll("Bearer [REDACTED]");
        if (sanitized.contains("base64Content")) {
            sanitized = BASE64_JSON.matcher(sanitized).replaceAll("\"base64Content\":\"[REDACTED]\"");
        }
        return sanitized.trim();
    }

    private static String sanitizeResponse(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return BEARER_HEADER.matcher(value).replaceAll("Bearer [REDACTED]").trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "-";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
