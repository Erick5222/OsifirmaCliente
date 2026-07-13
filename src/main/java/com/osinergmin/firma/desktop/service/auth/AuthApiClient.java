package com.osinergmin.firma.desktop.service.auth;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.osinergmin.firma.desktop.config.ClientConfiguration;
import com.osinergmin.firma.desktop.service.auth.dto.AuthApiResponseDto;
import com.osinergmin.firma.desktop.service.auth.dto.TokenDataDto;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class AuthApiClient {

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isServiceReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ClientConfiguration.getAuthApiBaseUrl()))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<Void> response = HTTP.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            return code > 0 && code < 600;
        } catch (Exception ex) {
            return false;
        }
    }

    public TokenValidationResult validateToken(String accessToken) {
        try {
            String encoded = URLEncoder.encode(accessToken.trim(), StandardCharsets.UTF_8);
            String url = ClientConfiguration.getAuthValidateTokenUrl() + "?token=" + encoded;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "*/*")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return mapValidationResponse(response.statusCode(), response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return TokenValidationResult.fail("Validacion de token cancelada.");
        } catch (Exception ex) {
            return TokenValidationResult.fail("No se pudo validar el token: " + ex.getMessage());
        }
    }

    public LoginResult login(String username, String password) {
        try {
            String url = ClientConfiguration.getAuthLoginUrl();
            String body = toJsonLoginBody(username, password);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return mapResponse(response.statusCode(), response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return LoginResult.fail("Inicio de sesion cancelado.");
        } catch (Exception ex) {
            return LoginResult.fail("No se pudo conectar con el servicio de autenticacion ("
                    + ClientConfiguration.getAuthLoginUrl() + "): " + ex.getMessage());
        }
    }

    private static String toJsonLoginBody(String username, String password) {
        return "{\"username\":" + jsonString(username) + ",\"password\":" + jsonString(password) + "}";
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    private LoginResult mapResponse(int httpStatus, String body) {
        if (body == null || body.isBlank()) {
            return LoginResult.fail("Respuesta vacia del servicio (HTTP " + httpStatus + ").");
        }
        try {
            AuthApiResponseDto wrapper = GSON.fromJson(body, AuthApiResponseDto.class);
            if (wrapper == null) {
                return LoginResult.fail("Respuesta invalida del servicio.");
            }
            int status = wrapper.getStatusCode() != null ? wrapper.getStatusCode() : httpStatus;
            if (status == 200 && wrapper.getData() != null && wrapper.getData().isJsonObject()) {
                TokenDataDto token = GSON.fromJson(wrapper.getData(), TokenDataDto.class);
                if (token != null && token.getAccessToken() != null && !token.getAccessToken().isBlank()) {
                    String msg = wrapper.getMessage() != null ? wrapper.getMessage() : "Autenticacion exitosa";
                    return LoginResult.ok(msg, token.getAccessToken(), token.getRefreshToken());
                }
            }
            return LoginResult.fail(resolveErrorMessage(wrapper, status));
        } catch (JsonSyntaxException ex) {
            return LoginResult.fail("No se pudo interpretar la respuesta del servicio.");
        }
    }

    private TokenValidationResult mapValidationResponse(int httpStatus, String body) {
        if (body == null || body.isBlank()) {
            return TokenValidationResult.fail("Respuesta vacia al validar token (HTTP " + httpStatus + ").");
        }
        try {
            AuthApiResponseDto wrapper = GSON.fromJson(body, AuthApiResponseDto.class);
            if (wrapper == null) {
                return TokenValidationResult.fail("Respuesta invalida al validar token.");
            }
            int status = wrapper.getStatusCode() != null ? wrapper.getStatusCode() : httpStatus;
            if (status == 200) {
                String msg = wrapper.getMessage() != null ? wrapper.getMessage() : "Token valido";
                return TokenValidationResult.ok(msg);
            }
            return TokenValidationResult.fail(resolveValidationErrorMessage(wrapper, status));
        } catch (JsonSyntaxException ex) {
            return TokenValidationResult.fail("No se pudo interpretar la respuesta de validacion.");
        }
    }

    private String resolveValidationErrorMessage(AuthApiResponseDto wrapper, int status) {
        String message = wrapper.getMessage();
        if (message != null && (message.contains("invalid_token") || message.contains("Token verification failed"))) {
            return "Token invalido o expirado.";
        }
        if (message != null && !message.isBlank()) {
            return message.length() > 200 ? message.substring(0, 200) + "..." : message;
        }
        return status == 401 ? "Token invalido o expirado." : "Error al validar token (HTTP " + status + ").";
    }

    private String resolveErrorMessage(AuthApiResponseDto wrapper, int status) {
        String message = wrapper.getMessage();
        if (message != null && (message.contains("Invalid user credentials") || message.contains("invalid_grant"))) {
            return "Usuario o contrasena incorrectos.";
        }
        if (message != null && !message.isBlank()) {
            return message.length() > 200 ? message.substring(0, 200) + "..." : message;
        }
        return status == 401 ? "Usuario o contrasena incorrectos." : "Error de autenticacion (HTTP " + status + ").";
    }
}
