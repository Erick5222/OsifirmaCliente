package com.osinergmin.firma.desktop.service.audit;

import com.osinergmin.firma.desktop.config.ClientConfiguration;
import com.osinergmin.firma.desktop.core.auth.AuthSession;
import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;
import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;
import com.osinergmin.firma.desktop.service.audit.dto.BitacoraEventoRequestDto;
import com.osinergmin.firma.desktop.service.certificate.InstalledCertificate;

import java.io.IOException;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Publica eventos de firma al servicio {@code POST /api/bitacora/evento}.
 * Fire-and-forget: no bloquea la UI ni interrumpe el flujo de firma.
 */
public final class BitacoraEventPublisher {

    private static final String COMPONENT_VERSION = "2.4.0-STABLE";
    private static final Pattern DNI_PATTERN = Pattern.compile("(\\d{8})");
    private static final BitacoraEventoClient CLIENT = new BitacoraEventoClient();
    private static volatile Consumer<String> uiLogSink;

    private BitacoraEventPublisher() {}

    /** Permite reflejar mensajes en el area de log de la pantalla de firma. */
    public static void setUiLogSink(Consumer<String> sink) {
        uiLogSink = sink;
    }

    public static void dispatchSignStart(InvokeConfig invokeConfig, InstalledCertificate certificate) {
        publishAsync("SIGN_START", invokeConfig, certificate, null, null, null, null);
    }

    public static void dispatchSignSuccess(
            InvokeConfig invokeConfig,
            InstalledCertificate certificate,
            PdfViewerConfig signedDocument,
            byte[] originalBytes,
            String message) {
        publishAsync("SIGN_OK", invokeConfig, certificate, signedDocument, originalBytes, message, null);
    }

    public static void dispatchSignError(
            InvokeConfig invokeConfig, InstalledCertificate certificate, String errorMessage) {
        publishAsync("SIGN_ERROR", invokeConfig, certificate, null, null, null, errorMessage);
    }

    private static void publishAsync(
            String tipoEvento,
            InvokeConfig invokeConfig,
            InstalledCertificate certificate,
            PdfViewerConfig signedDocument,
            byte[] originalBytes,
            String successMessage,
            String errorMessage) {
        if (!ClientConfiguration.isBitacoraEnabled()) {
            log("Bitacora deshabilitada (bitacora.enabled=false). No se envia " + tipoEvento + ".", true);
            return;
        }

        String url = ClientConfiguration.getBitacoraEventoUrl();
        String coTransaccion = resolveCoTransaccion(invokeConfig);
        log(
                "Programando evento "
                        + tipoEvento
                        + " -> POST "
                        + url
                        + " | coTransaccion="
                        + coTransaccion,
                false);

        dispatchAsync(
                () -> {
                    try {
                        String token = resolveAccessToken(invokeConfig);
                        if (token.isBlank()) {
                            log(
                                    "OMITIDO "
                                            + tipoEvento
                                            + ": sin token Bearer. Defina FIRMA_JWT_TOKEN, -Dfirma.jwt.token "
                                            + "o inicie sesion en el firmador.",
                                    true);
                            return;
                        }

                        BitacoraEventoRequestDto dto = baseEvent(invokeConfig, certificate);
                        dto.coTransaccion = coTransaccion;
                        dto.coTipoEvento = tipoEvento;
                        if ("SIGN_ERROR".equals(tipoEvento)) {
                            dto.coEstado = "ERROR";
                            dto.deMensaje = "Error en proceso de firma desktop";
                            dto.deError = truncate(errorMessage, 2000);
                        } else {
                            dto.coEstado = "OK";
                            if ("SIGN_START".equals(tipoEvento)) {
                                dto.deMensaje = "Inicio de proceso de firma en desktop";
                            } else {
                                dto.deMensaje = truncate(successMessage, 500);
                                fillDocumentMetadata(dto, signedDocument, originalBytes, invokeConfig);
                            }
                        }

                        log(
                                "Enviando "
                                        + tipoEvento
                                        + " (token="
                                        + tokenSourceLabel(invokeConfig)
                                        + ")...",
                                false);
                        BitacoraEventoResult result = CLIENT.postEvento(token, dto);
                        if (result.success()) {
                            log(
                                    "OK "
                                            + tipoEvento
                                            + " registrado en servicio (HTTP "
                                            + result.httpStatus()
                                            + ").",
                                    false);
                        } else {
                            log(
                                    "FALLO "
                                            + tipoEvento
                                            + ": "
                                            + result.message()
                                            + (result.httpStatus() > 0
                                                    ? " (HTTP " + result.httpStatus() + ")"
                                                    : ""),
                                    true);
                        }
                    } catch (Exception ex) {
                        log("EXCEPCION enviando " + tipoEvento + ": " + ex.getMessage(), true);
                        ex.printStackTrace();
                    }
                });
    }

    private static String tokenSourceLabel(InvokeConfig invokeConfig) {
        if (AuthSession.getAccessToken().isPresent()) {
            return "AuthSession";
        }
        if (invokeConfig != null && !invokeConfig.getToken().isBlank()) {
            return "parametria";
        }
        return "ninguno";
    }

    private static BitacoraEventoRequestDto baseEvent(
            InvokeConfig invokeConfig, InstalledCertificate certificate) {
        BitacoraEventoRequestDto dto = new BitacoraEventoRequestDto();
        dto.coComponente = "DESKTOP";
        dto.coVersion = COMPONENT_VERSION;
        dto.coIpOrigen = localIp();
        if (invokeConfig != null) {
            dto.coOrigenDocumento = invokeConfig.getOrigenDocumento();
            if (invokeConfig.usesAlfrescoDocument()) {
                dto.coNodeAlfresco = invokeConfig.getIdDocumentoAlfresco();
            }
            SigningParam param = invokeConfig.signingParam();
            if (param != null) {
                dto.coFormatoFirma = param.standardFirma();
                dto.coNivelFirma = param.signatureLevel();
            }
        }
        if (certificate != null) {
            dto.coCertificado = certificate.libraryAlias();
            dto.noFirmante = certificate.titular();
            dto.coDniFirmante = extractDni(certificate.libraryAlias());
        }
        return dto;
    }

    private static void fillDocumentMetadata(
            BitacoraEventoRequestDto dto,
            PdfViewerConfig signedDocument,
            byte[] originalBytes,
            InvokeConfig invokeConfig) {
        if (originalBytes != null && originalBytes.length > 0) {
            dto.coHashOriginal = sha256(originalBytes);
        }
        if (signedDocument == null) {
            return;
        }
        dto.noArchivo = signedDocument.displayFileName();
        try {
            byte[] signedBytes = signedDocument.readDocumentBytes();
            dto.nuTamanio = (long) signedBytes.length;
            dto.coHashFirmado = sha256(signedBytes);
        } catch (IOException ignored) {
            dto.nuTamanio = 0L;
        }
        if (invokeConfig != null && invokeConfig.usesAlfrescoDocument()) {
            dto.coNodeAlfresco = invokeConfig.getIdDocumentoAlfresco();
        }
    }

    private static void dispatchAsync(Runnable action) {
        Thread thread = new Thread(action, "firmador-bitacora");
        thread.setDaemon(true);
        thread.start();
    }

    private static void log(String message, boolean error) {
        String line = "[Bitacora] " + message;
        if (error) {
            System.err.println(line);
        } else {
            System.out.println(line);
        }
        Consumer<String> sink = uiLogSink;
        if (sink != null) {
            try {
                sink.accept(error ? "BITACORA ERROR: " + message : "Bitacora: " + message);
            } catch (Exception ex) {
                System.err.println("[Bitacora] No se pudo escribir en UI log: " + ex.getMessage());
            }
        }
    }

    private static String resolveAccessToken(InvokeConfig invokeConfig) {
        return AuthSession.getAccessToken()
                .orElseGet(
                        () ->
                                invokeConfig != null && !invokeConfig.getToken().isBlank()
                                        ? invokeConfig.getToken()
                                        : "");
    }

    private static String resolveCoTransaccion(InvokeConfig invokeConfig) {
        if (invokeConfig != null && !invokeConfig.getIdSolicitud().isBlank()) {
            return invokeConfig.getIdSolicitud().trim();
        }
        return "TXN-" + Instant.now().toEpochMilli();
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
        Matcher matcher = DNI_PATTERN.matcher(alias);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String localIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ex) {
            return "127.0.0.1";
        }
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
