package com.osinergmin.firma.desktop.service.signing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.osinergmin.firma.desktop.core.auth.AuthSession;
import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;
import com.osinergmin.firma.desktop.service.certificate.InstalledCertificate;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

public final class SigningCallbackClient {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final String COMPONENT_VERSION = "2.4.0-STABLE";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private SigningCallbackClient() {}

    public static Optional<String> postIfConfigured(
            InvokeConfig invokeConfig,
            boolean success,
            String errorMessage,
            byte[] originalBytes,
            PdfViewerConfig signedDocument,
            InstalledCertificate certificate) {
        if (invokeConfig == null || !invokeConfig.hasCallbackUrl()) {
            return Optional.empty();
        }
        String payload =
                buildPayload(invokeConfig, success, errorMessage, originalBytes, signedDocument, certificate);
        try {
            HttpRequest.Builder builder =
                    HttpRequest.newBuilder()
                            .uri(URI.create(invokeConfig.getCallbackUrl().trim()))
                            .timeout(java.time.Duration.ofSeconds(30))
                            .header("Content-Type", "application/json; charset=UTF-8")
                            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
            AuthSession.getAccessToken()
                    .or(() -> Optional.ofNullable(invokeConfig.getToken()).filter(t -> !t.isBlank()))
                    .ifPresent(token -> builder.header("Authorization", "Bearer " + token.trim()));
            HttpResponse<String> response =
                    CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Optional.of("Callback enviado (" + response.statusCode() + ").");
            }
            return Optional.of("Callback respondio HTTP " + response.statusCode() + ".");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.of("Callback cancelado.");
        } catch (IOException ex) {
            return Optional.of("No se pudo enviar callback: " + ex.getMessage());
        }
    }

    static String buildPayload(
            InvokeConfig invokeConfig,
            boolean success,
            String errorMessage,
            byte[] originalBytes,
            PdfViewerConfig signedDocument,
            InstalledCertificate certificate) {
        CallbackRootDto root = new CallbackRootDto();
        root.status = success ? "SUCCESS" : "ERROR";
        root.errorCode = success ? null : "SIGN_ERROR";
        root.errorMessage = success ? null : nullToEmpty(errorMessage);

        if (success && signedDocument != null) {
            root.document = new CallbackDocumentDto();
            root.document.originalHash = sha256(originalBytes);
            try {
                byte[] signedBytes = signedDocument.readDocumentBytes();
                root.document.signedHash = sha256(signedBytes);
                root.document.fileName = signedDocument.displayFileName();
                root.document.fileSize = signedBytes.length;
                root.document.signedContentBase64 = Base64.getEncoder().encodeToString(signedBytes);
            } catch (IOException ex) {
                root.document.signedHash = "";
                root.document.fileName = signedDocument.displayFileName();
                root.document.fileSize = 0;
                root.document.signedContentBase64 = "";
            }
            root.document.mimeType =
                    SigningDocumentNaming.guessMimeType(signedDocument.displayFileName());
        }

        root.signer = new CallbackSignerDto();
        if (certificate != null) {
            root.signer.fullName = nullToEmpty(certificate.titular());
            root.signer.organization = nullToEmpty(certificate.emisor());
            root.signer.dni = extractDni(certificate.libraryAlias());
        } else {
            root.signer.fullName = "";
            root.signer.dni = "";
            root.signer.organization = "OSI";
        }
        root.signer.email = "";

        root.transaction = new CallbackTransactionDto();
        root.transaction.transactionId =
                invokeConfig.getIdSolicitud().isBlank()
                        ? "TXN-" + Instant.now().toEpochMilli()
                        : invokeConfig.getIdSolicitud();
        root.transaction.sessionId = AuthSession.getAccessToken().orElse(invokeConfig.getToken());
        root.transaction.ipAddress = localIp();
        root.transaction.componentVersion = COMPONENT_VERSION;
        root.transaction.signedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        return GSON.toJson(root);
    }

    private static String sha256(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            return "";
        }
    }

    private static String extractDni(String alias) {
        if (alias == null) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{8})").matcher(alias);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String localIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ex) {
            return "127.0.0.1";
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class CallbackRootDto {
        String status;
        String errorCode;
        String errorMessage;
        CallbackDocumentDto document;
        CallbackSignerDto signer;
        CallbackTransactionDto transaction;
    }

    private static final class CallbackDocumentDto {
        String originalHash;
        String signedHash;
        String fileName;
        int fileSize;
        String mimeType;
        String signedContentBase64;
    }

    private static final class CallbackSignerDto {
        String dni;
        String fullName;
        String email;
        String organization;
    }

    private static final class CallbackTransactionDto {
        String transactionId;
        String sessionId;
        String ipAddress;
        String componentVersion;
        String signedAt;
    }
}
