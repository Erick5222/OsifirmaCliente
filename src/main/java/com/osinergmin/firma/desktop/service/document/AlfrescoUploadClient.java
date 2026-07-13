package com.osinergmin.firma.desktop.service.document;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.osinergmin.firma.desktop.config.ClientConfiguration;
import com.osinergmin.firma.desktop.core.audit.HttpServiceAuditLog;
import com.osinergmin.firma.desktop.service.auth.dto.AuthApiResponseDto;
import com.osinergmin.firma.desktop.service.document.dto.AlfrescoUploadRequestDto;
import com.osinergmin.firma.desktop.service.signing.SigningDocumentNaming;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

/** Sube el documento firmado (PDF, .p7s, .xml, etc.) al repositorio Alfresco via firma-digital-service. */
public final class AlfrescoUploadClient {

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public AlfrescoUploadResult uploadPdf(
            String nodeId, String accessToken, String fileName, byte[] pdfBytes, Integer sourceVersion) {
        return uploadSignedDocument(nodeId, accessToken, fileName, pdfBytes, "application/pdf", sourceVersion);
    }

    public AlfrescoUploadResult uploadSignedDocument(
            String nodeId,
            String accessToken,
            String fileName,
            byte[] contentBytes,
            String contentType,
            Integer sourceVersion) {
        if (nodeId == null || nodeId.isBlank()) {
            return AlfrescoUploadResult.fail("No hay nodeId de destino Alfresco.");
        }
        if (accessToken == null || accessToken.isBlank()) {
            return AlfrescoUploadResult.fail("No hay token de sesion para subir a Alfresco.");
        }
        if (contentBytes == null || contentBytes.length == 0) {
            return AlfrescoUploadResult.fail("El documento firmado esta vacio.");
        }

        String resolvedName = normalizeUploadFileName(fileName);
        String mimeType =
                contentType != null && !contentType.isBlank()
                        ? contentType
                        : SigningDocumentNaming.guessMimeType(resolvedName);
        String payload =
                GSON.toJson(
                        new AlfrescoUploadRequestDto(
                                resolvedName,
                                Base64.getEncoder().encodeToString(contentBytes),
                                mimeType,
                                sourceVersion));

        try {
            String url = ClientConfiguration.getAlfrescoUploadUrl(nodeId.trim());
            System.out.println(
                    "[AlfrescoUploadClient] POST " + url
                            + " | fileName=" + resolvedName
                            + " | sourceVersion="
                            + sourceVersion
                            + " | bytes="
                            + contentBytes.length
                            + " | contentType="
                            + mimeType);
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(120))
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json; charset=UTF-8")
                            .header("Authorization", "Bearer " + accessToken.trim())
                            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                            .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            AlfrescoUploadResult result = mapResponse(response.statusCode(), response.body());
            if (result.success()) {
                System.out.println("[AlfrescoUploadClient] OK HTTP " + response.statusCode() + " -> " + result.message());
                HttpServiceAuditLog.recordSuccess(
                        "AlfrescoUploadClient",
                        "POST",
                        url,
                        buildRequestSummary(resolvedName, sourceVersion, contentBytes.length, mimeType),
                        response.statusCode(),
                        response.body());
            } else {
                System.err.println(
                        "[AlfrescoUploadClient] ERROR HTTP " + response.statusCode() + " -> " + result.message());
                HttpServiceAuditLog.recordFailure(
                        "AlfrescoUploadClient",
                        "POST",
                        url,
                        payload,
                        response.statusCode(),
                        response.body(),
                        result.message());
                System.err.println("[AlfrescoUploadClient] Detalle en: " + HttpServiceAuditLog.getLogFilePath());
            }
            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.err.println("[AlfrescoUploadClient] Subida cancelada.");
            HttpServiceAuditLog.recordFailure(
                    "AlfrescoUploadClient", "POST", ClientConfiguration.getAlfrescoUploadUrl(nodeId.trim()), payload, "Subida cancelada.");
            return AlfrescoUploadResult.fail("Subida a Alfresco cancelada.");
        } catch (Exception ex) {
            System.err.println("[AlfrescoUploadClient] Fallo de red: " + ex.getMessage());
            HttpServiceAuditLog.recordFailure(
                    "AlfrescoUploadClient",
                    "POST",
                    ClientConfiguration.getAlfrescoUploadUrl(nodeId.trim()),
                    payload,
                    ex.getMessage());
            System.err.println("[AlfrescoUploadClient] Detalle en: " + HttpServiceAuditLog.getLogFilePath());
            return AlfrescoUploadResult.fail("No se pudo subir a Alfresco: " + ex.getMessage());
        }
    }

    private static String buildRequestSummary(
            String fileName, Integer sourceVersion, int byteCount, String contentType) {
        return "fileName="
                + fileName
                + ", contentType="
                + contentType
                + ", sourceVersion="
                + sourceVersion
                + ", targetVersion="
                + (sourceVersion != null && sourceVersion > 0 ? sourceVersion + 1 : "-")
                + ", bytes="
                + byteCount
                + ", base64Content=[REDACTED]";
    }

    private AlfrescoUploadResult mapResponse(int httpStatus, String body) {
        if (httpStatus >= 200 && httpStatus < 300 && (body == null || body.isBlank())) {
            return AlfrescoUploadResult.ok("Documento actualizado en Alfresco.");
        }
        if (body == null || body.isBlank()) {
            return AlfrescoUploadResult.fail("Respuesta vacia del servicio (HTTP " + httpStatus + ").");
        }
        try {
            AuthApiResponseDto wrapper = GSON.fromJson(body, AuthApiResponseDto.class);
            if (wrapper == null) {
                return AlfrescoUploadResult.fail("Respuesta invalida del servicio.");
            }
            int status = wrapper.getStatusCode() != null ? wrapper.getStatusCode() : httpStatus;
            if (status >= 200 && status < 300) {
                String message = wrapper.getMessage();
                return AlfrescoUploadResult.ok(
                        message != null && !message.isBlank()
                                ? message.trim()
                                : "Documento actualizado en Alfresco.");
            }
            return AlfrescoUploadResult.fail(resolveErrorMessage(wrapper, status));
        } catch (JsonSyntaxException ex) {
            if (httpStatus >= 200 && httpStatus < 300) {
                return AlfrescoUploadResult.ok("Documento actualizado en Alfresco.");
            }
            return AlfrescoUploadResult.fail("No se pudo interpretar la respuesta del servicio.");
        }
    }

    private static String normalizeUploadFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "documento[F].pdf";
        }
        return fileName.trim();
    }

    private static String resolveErrorMessage(AuthApiResponseDto wrapper, int status) {
        String message = wrapper.getMessage();
        if (message != null && !message.isBlank()) {
            return message.length() > 240 ? message.substring(0, 240) + "..." : message;
        }
        if (status == 401 || status == 403) {
            return "No autorizado para subir el documento (HTTP " + status + ").";
        }
        return "Error al subir documento (HTTP " + status + ").";
    }
}
