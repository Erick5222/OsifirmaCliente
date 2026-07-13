package com.osinergmin.firma.desktop.service.document;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/** Descarga PDF desde URL temporal expuesta por el portal de invocacion. */
public final class DocumentUrlClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final HttpClient CLIENT =
            HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build();

    private DocumentUrlClient() {}

    public static DocumentUrlResult download(String url, Optional<String> bearerToken) {
        if (url == null || url.isBlank()) {
            return DocumentUrlResult.fail("No hay URL de documento.");
        }
        try {
            HttpRequest.Builder builder =
                    HttpRequest.newBuilder().uri(URI.create(url.trim())).timeout(TIMEOUT).GET();
            bearerToken.filter(token -> !token.isBlank())
                    .ifPresent(token -> builder.header("Authorization", "Bearer " + token.trim()));
            HttpResponse<byte[]> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return DocumentUrlResult.fail("Descarga HTTP " + response.statusCode());
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                return DocumentUrlResult.fail("La URL no devolvio contenido.");
            }
            String fileName = extractFileName(url);
            return DocumentUrlResult.success(body, fileName);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return DocumentUrlResult.fail("Descarga cancelada.");
        } catch (IOException | IllegalArgumentException ex) {
            return DocumentUrlResult.fail("No se pudo descargar el documento: " + ex.getMessage());
        }
    }

    private static String extractFileName(String url) {
        try {
            String path = URI.create(url.trim()).getPath();
            if (path == null || path.isBlank()) {
                return "documento.pdf";
            }
            int slash = path.lastIndexOf('/');
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            return name.isBlank() ? "documento.pdf" : name;
        } catch (IllegalArgumentException ex) {
            return "documento.pdf";
        }
    }
}
