package com.osinergmin.firma.desktop.service.audit;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.osinergmin.firma.desktop.config.ClientConfiguration;
import com.osinergmin.firma.desktop.core.audit.HttpServiceAuditLog;
import com.osinergmin.firma.desktop.service.audit.dto.BitacoraEventoRequestDto;
import com.osinergmin.firma.desktop.service.auth.dto.AuthApiResponseDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class BitacoraEventoClient {

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public BitacoraEventoResult postEvento(String accessToken, BitacoraEventoRequestDto request) {
        if (!ClientConfiguration.isBitacoraEnabled()) {
            System.out.println("[BitacoraEventoClient] Bitacora deshabilitada en cliente.");
            return BitacoraEventoResult.fail("Bitacora deshabilitada en cliente.", 0);
        }
        if (accessToken == null || accessToken.isBlank()) {
            System.err.println("[BitacoraEventoClient] Sin token Bearer para bitacora.");
            return BitacoraEventoResult.fail("Sin token Bearer para bitacora.", 0);
        }
        if (request == null) {
            return BitacoraEventoResult.fail("Payload de bitacora vacio.", 0);
        }

        String payload = GSON.toJson(request);
        String url = ClientConfiguration.getBitacoraEventoUrl();
        String requestSummary = buildRequestSummary(request);

        System.out.println(
                "[BitacoraEventoClient] POST "
                        + url
                        + " | "
                        + requestSummary);

        try {
            HttpRequest httpRequest =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(20))
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json; charset=UTF-8")
                            .header("Authorization", "Bearer " + accessToken.trim())
                            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                            .build();
            HttpResponse<String> response = HTTP.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            BitacoraEventoResult result = mapResponse(response.statusCode(), response.body());
            if (result.success()) {
                System.out.println(
                        "[BitacoraEventoClient] OK HTTP "
                                + response.statusCode()
                                + " -> "
                                + result.message());
                HttpServiceAuditLog.recordSuccess(
                        "BitacoraEventoClient",
                        "POST",
                        url,
                        requestSummary,
                        response.statusCode(),
                        response.body());
            } else {
                System.err.println(
                        "[BitacoraEventoClient] ERROR HTTP "
                                + response.statusCode()
                                + " -> "
                                + result.message());
                if (response.body() != null && !response.body().isBlank()) {
                    System.err.println("[BitacoraEventoClient] body=" + truncateBody(response.body()));
                }
                HttpServiceAuditLog.recordFailure(
                        "BitacoraEventoClient",
                        "POST",
                        url,
                        requestSummary,
                        response.statusCode(),
                        response.body(),
                        result.message());
            }
            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.err.println("[BitacoraEventoClient] Bitacora cancelada.");
            HttpServiceAuditLog.recordFailure(
                    "BitacoraEventoClient", "POST", url, requestSummary, "Bitacora cancelada.");
            return BitacoraEventoResult.fail("Bitacora cancelada.", 0);
        } catch (Exception ex) {
            System.err.println("[BitacoraEventoClient] Fallo de red: " + ex.getMessage());
            HttpServiceAuditLog.recordFailure(
                    "BitacoraEventoClient", "POST", url, requestSummary, ex.getMessage());
            return BitacoraEventoResult.fail("No se pudo registrar bitacora: " + ex.getMessage(), 0);
        }
    }

    private static String buildRequestSummary(BitacoraEventoRequestDto request) {
        return "coTransaccion="
                + nullToEmpty(request.coTransaccion)
                + ", coTipoEvento="
                + nullToEmpty(request.coTipoEvento)
                + ", coEstado="
                + nullToEmpty(request.coEstado)
                + ", noArchivo="
                + nullToEmpty(request.noArchivo);
    }

    private BitacoraEventoResult mapResponse(int httpStatus, String body) {
        if (httpStatus >= 200 && httpStatus < 300) {
            return BitacoraEventoResult.ok("Evento registrado (HTTP " + httpStatus + ").", httpStatus);
        }
        if (body == null || body.isBlank()) {
            return BitacoraEventoResult.fail("Error bitacora HTTP " + httpStatus + ".", httpStatus);
        }
        try {
            AuthApiResponseDto wrapper = GSON.fromJson(body, AuthApiResponseDto.class);
            if (wrapper != null && wrapper.getMessage() != null && !wrapper.getMessage().isBlank()) {
                return BitacoraEventoResult.fail(wrapper.getMessage().trim(), httpStatus);
            }
        } catch (JsonSyntaxException ignored) {
            // cuerpo no JSON
        }
        return BitacoraEventoResult.fail("Error bitacora HTTP " + httpStatus + ".", httpStatus);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String truncateBody(String body) {
        if (body.length() <= 300) {
            return body;
        }
        return body.substring(0, 300) + "...";
    }
}
