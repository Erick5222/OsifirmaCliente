package com.osinergmin.firma.desktop.service.document;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.osinergmin.firma.desktop.config.ClientConfiguration;
import com.osinergmin.firma.desktop.core.audit.HttpServiceAuditLog;
import com.osinergmin.firma.desktop.service.auth.dto.AuthApiResponseDto;
import com.osinergmin.firma.desktop.service.document.dto.AlfrescoContentDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

public final class AlfrescoContentClient {

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public AlfrescoContentResult fetchPdfContent(String nodeId, String accessToken) {
        return fetchPdfContent(nodeId, accessToken, null);
    }

    public AlfrescoContentResult fetchPdfContent(String nodeId, String accessToken, Integer version) {
        if (nodeId == null || nodeId.isBlank()) {
            return AlfrescoContentResult.fail("No hay nodeId de Alfresco en parametria.");
        }
        if (accessToken == null || accessToken.isBlank()) {
            return AlfrescoContentResult.fail("No hay token de sesion para consultar Alfresco.");
        }

        try {
            String url = ClientConfiguration.getAlfrescoContentUrl(nodeId.trim(), version);
            System.out.println("[AlfrescoContentClient] GET " + url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Accept", "*/*")
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            AlfrescoContentResult result = mapResponse(response.statusCode(), response.body());
            if (result.success()) {
                HttpServiceAuditLog.recordSuccess(
                        "AlfrescoContentClient",
                        "GET",
                        url,
                        "nodeId=" + nodeId + ", version=" + version,
                        response.statusCode(),
                        "pdfBytes=" + result.pdfBytes().length + ", fileName=" + result.fileName());
            } else {
                HttpServiceAuditLog.recordFailure(
                        "AlfrescoContentClient",
                        "GET",
                        url,
                        "nodeId=" + nodeId + ", version=" + version,
                        response.statusCode(),
                        response.body(),
                        result.message());
                System.err.println("[AlfrescoContentClient] Detalle en: " + HttpServiceAuditLog.getLogFilePath());
            }
            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            HttpServiceAuditLog.recordFailure(
                    "AlfrescoContentClient",
                    "GET",
                    ClientConfiguration.getAlfrescoContentUrl(nodeId.trim(), version),
                    "nodeId=" + nodeId + ", version=" + version,
                    "Descarga cancelada.");
            return AlfrescoContentResult.fail("Descarga de documento cancelada.");
        } catch (Exception ex) {
            HttpServiceAuditLog.recordFailure(
                    "AlfrescoContentClient",
                    "GET",
                    ClientConfiguration.getAlfrescoContentUrl(nodeId.trim(), version),
                    "nodeId=" + nodeId + ", version=" + version,
                    ex.getMessage());
            System.err.println("[AlfrescoContentClient] Detalle en: " + HttpServiceAuditLog.getLogFilePath());
            return AlfrescoContentResult.fail("No se pudo obtener el documento de Alfresco: " + ex.getMessage());
        }
    }

    private AlfrescoContentResult mapResponse(int httpStatus, String body) {
        if (body == null || body.isBlank()) {
            return AlfrescoContentResult.fail("Respuesta vacia del servicio (HTTP " + httpStatus + ").");
        }
        try {
            AuthApiResponseDto wrapper = GSON.fromJson(body, AuthApiResponseDto.class);
            if (wrapper == null) {
                return AlfrescoContentResult.fail("Respuesta invalida del servicio.");
            }
            int status = wrapper.getStatusCode() != null ? wrapper.getStatusCode() : httpStatus;
            if (status != 200 || wrapper.getData() == null || !wrapper.getData().isJsonObject()) {
                return AlfrescoContentResult.fail(resolveErrorMessage(wrapper, status));
            }

            AlfrescoContentDto data = GSON.fromJson(wrapper.getData(), AlfrescoContentDto.class);
            if (data == null || data.getBase64Content() == null || data.getBase64Content().isBlank()) {
                return AlfrescoContentResult.fail("El servicio no devolvio contenido del documento.");
            }

            String contentType = data.getContentType() != null ? data.getContentType() : "";
            if (!isSupportedContentType(contentType)) {
                return AlfrescoContentResult.fail(
                        "Tipo de documento no soportado para firma (contentType: " + contentType + ").");
            }

            byte[] bytes = Base64.getDecoder().decode(data.getBase64Content().trim());
            if (bytes.length == 0) {
                return AlfrescoContentResult.fail("El contenido del documento esta vacio.");
            }

            String fileName = data.getFileName() != null && !data.getFileName().isBlank()
                    ? data.getFileName().trim()
                    : defaultFileNameForContentType(contentType);
            return AlfrescoContentResult.ok(bytes, fileName, contentType);
        } catch (IllegalArgumentException ex) {
            return AlfrescoContentResult.fail("Contenido Base64 invalido en la respuesta.");
        } catch (JsonSyntaxException ex) {
            return AlfrescoContentResult.fail("No se pudo interpretar la respuesta del servicio.");
        }
    }

    private static boolean isSupportedContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return true;
        }
        String base = baseMimeType(contentType);
        return base.contains("application/pdf")
                || base.endsWith("/pdf")
                || base.contains("application/xml")
                || base.contains("/xml")
                || base.contains("wordprocessingml")
                || base.contains("msword")
                || base.contains("application/octet-stream")
                || base.startsWith("image/");
    }

    private static String baseMimeType(String contentType) {
        String lower = contentType.toLowerCase(Locale.ROOT).trim();
        int semi = lower.indexOf(';');
        return semi >= 0 ? lower.substring(0, semi).trim() : lower;
    }

    private static String defaultFileNameForContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "documento_alfresco.pdf";
        }
        String base = baseMimeType(contentType);
        if (base.contains("xml")) {
            return "documento_alfresco.xml";
        }
        if (base.contains("wordprocessingml") || base.contains("msword")) {
            return "documento_alfresco.docx";
        }
        if (base.contains("jpeg") || base.contains("jpg")) {
            return "documento_alfresco.jpg";
        }
        if (base.contains("png")) {
            return "documento_alfresco.png";
        }
        if (base.startsWith("image/")) {
            return "documento_alfresco.bin";
        }
        return "documento_alfresco.pdf";
    }

    private static String resolveErrorMessage(AuthApiResponseDto wrapper, int status) {
        String message = wrapper.getMessage();
        if (message != null && !message.isBlank()) {
            return message.length() > 240 ? message.substring(0, 240) + "..." : message;
        }
        if (status == 401 || status == 403) {
            return "No autorizado para obtener el documento (HTTP " + status + ").";
        }
        return "Error al obtener documento (HTTP " + status + ").";
    }
}
