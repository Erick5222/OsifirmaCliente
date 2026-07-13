package com.osinergmin.firma.desktop.integration.invoke;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.osinergmin.firma.desktop.config.SigningAdminSettings;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public final class ParametriaLoader {

    private static final Gson GSON = new Gson();

    private ParametriaLoader() {}

    /** Bloque {@code param} enviado por el simulador/navegador en la URL del protocolo ({@code paramB64}). */
    public static Optional<SigningParam> parseParamFromProtocol(Map<String, String> named) {
        if (named == null || named.isEmpty()) {
            return Optional.empty();
        }
        String b64 = firstNonBlank(named.get("paramB64"), named.get("param_b64"));
        if (b64 != null && !b64.isBlank()) {
            try {
                byte[] raw = Base64.getDecoder().decode(b64.trim());
                return parseParamJson(new String(raw, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ex) {
                System.err.println("[ParametriaLoader] paramB64 no es Base64 valido.");
            }
        }
        String json = named.get("paramJson");
        if (json != null && !json.isBlank()) {
            return parseParamJson(json);
        }
        return Optional.empty();
    }

    public static Optional<SigningParam> parseParamJson(String jsonUtf8) {
        if (jsonUtf8 == null || jsonUtf8.isBlank()) {
            return Optional.empty();
        }
        try {
            SigningParamDto dto = GSON.fromJson(jsonUtf8.trim(), SigningParamDto.class);
            if (dto == null) {
                return Optional.empty();
            }
            SigningParam param = mergeParamDto(dto);
            System.out.println(
                    "[ParametriaLoader] param desde protocolo -> formato="
                            + param.signatureFormat()
                            + " tag="
                            + (param.effectiveTagMarker().isBlank()
                                    ? "(vacio)"
                                    : param.effectiveTagMarker())
                            + " modoPos="
                            + param.positioningMode());
            return Optional.of(param);
        } catch (JsonSyntaxException ex) {
            System.err.println("[ParametriaLoader] JSON de param invalido: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<InvokeConfig> load(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            SigningInvokeRootDto root = GSON.fromJson(reader, SigningInvokeRootDto.class);
            if (root == null) {
                return Optional.empty();
            }
            return Optional.of(root.toInvokeConfig(file));
        } catch (IOException | JsonSyntaxException | IllegalArgumentException e) {
            System.err.println("[ParametriaLoader] No se pudo leer " + file + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private static final class SigningInvokeRootDto {
        SigningInvocacionDto invocacion;
        SigningParamDto param;
        String credential;
        String document;
        String stamp;

        String idSolicitud;
        String token;
        String refresh_token;
        String origenDocumento;
        String rutaLocal;
        String documentoUrl;
        String idDocumentoAlfresco;
        String nodeIdAlfresco;
        String contentType;
        Boolean usarAlfresco;
        Boolean sinVisor;
        Boolean sinBandeja;
        Boolean abrirVisorPrimero;
        String modo;
        Boolean FirmaArrastrar;
        Boolean cerrarAlTerminar;
        Boolean interfazMinima;
        String callbackUrl;
        String webOrigen;
        Integer documentVersion;

        InvokeConfig toInvokeConfig(Path parametriaFile) {
            SigningInvocacionDto inv = invocacion != null ? invocacion : legacyInvocacion();
            SigningParam signingParam = mergeParamDto(param);
            logSigningParamFromFile(parametriaFile, signingParam);
            byte[] documentBytes = decodeBase64(ParametriaVariables.resolve(document, parametriaFile), parametriaFile);
            byte[] stampBytes = decodeBase64(ParametriaVariables.resolve(stamp, parametriaFile), parametriaFile);

            String origen = firstNonBlank(inv.origenDocumento, origenDocumento, "local");
            if (documentBytes != null && documentBytes.length > 0) {
                origen = "bytes";
            }

            boolean alfresco =
                    Boolean.TRUE.equals(firstNonNull(inv.usarAlfresco, usarAlfresco))
                            || "alfresco".equalsIgnoreCase(origen);
            String alfrescoNode = resolveValue(
                    parametriaFile,
                    firstNonBlank(inv.nodeIdAlfresco, inv.idDocumentoAlfresco, idDocumentoAlfresco, nodeIdAlfresco));
            boolean firmaArrastrar =
                    resolveFirmaArrastrar(firstNonNull(inv.FirmaArrastrar, inv.firmaArrastrar, FirmaArrastrar));
            Integer alfrescoVersion =
                    resolveDocumentVersion(
                            firstNonNull(
                                    inv.documentVersion,
                                    param != null ? param.documentVersion : null,
                                    documentVersion));
            if (alfresco && !alfrescoNode.isBlank() && alfrescoVersion == null) {
                alfrescoVersion = 1;
            }

            return new InvokeConfig(
                    resolveValue(parametriaFile, firstNonBlank(inv.idSolicitud, idSolicitud)),
                    resolveValue(parametriaFile, firstNonBlank(inv.token, token)),
                    resolveValue(parametriaFile, firstNonBlank(inv.refresh_token, refresh_token)),
                    resolveValue(parametriaFile, firstNonBlank(inv.callbackUrl, callbackUrl)),
                    origen,
                    firstNonBlank(inv.rutaLocal, rutaLocal),
                    firstNonBlank(inv.documentoUrl, documentoUrl),
                    alfrescoNode,
                    firstNonBlank(inv.contentType, contentType, "application/pdf"),
                    alfresco,
                    Boolean.TRUE.equals(firstNonNull(inv.sinVisor, sinVisor)),
                    resolveSinBandeja(firstNonNull(inv.sinBandeja, sinBandeja)),
                    Boolean.TRUE.equals(firstNonNull(inv.abrirVisorPrimero, abrirVisorPrimero)),
                    firstNonBlank(inv.modo, modo, "completo"),
                    firmaArrastrar,
                    Boolean.TRUE.equals(firstNonNull(inv.cerrarAlTerminar, cerrarAlTerminar)),
                    Boolean.TRUE.equals(firstNonNull(inv.interfazMinima, interfazMinima)),
                    nullToEmpty(ParametriaVariables.resolve(credential, parametriaFile)),
                    signingParam,
                    alfrescoVersion,
                    documentBytes,
                    stampBytes);
        }

        private SigningInvocacionDto legacyInvocacion() {
            SigningInvocacionDto dto = new SigningInvocacionDto();
            dto.idSolicitud = idSolicitud;
            dto.token = token;
            dto.refresh_token = refresh_token;
            dto.origenDocumento = origenDocumento;
            dto.rutaLocal = rutaLocal;
            dto.documentoUrl = documentoUrl;
            dto.idDocumentoAlfresco = idDocumentoAlfresco;
            dto.nodeIdAlfresco = nodeIdAlfresco;
            dto.contentType = contentType;
            dto.usarAlfresco = usarAlfresco;
            dto.sinVisor = sinVisor;
            dto.sinBandeja = sinBandeja;
            dto.abrirVisorPrimero = abrirVisorPrimero;
            dto.modo = modo;
            dto.callbackUrl = callbackUrl;
            dto.FirmaArrastrar = FirmaArrastrar;
            dto.cerrarAlTerminar = cerrarAlTerminar;
            dto.interfazMinima = interfazMinima;
            dto.documentVersion = documentVersion;
            return dto;
        }

        private static boolean resolveFirmaArrastrar(Boolean value) {
            return value == null || value;
        }

        private static boolean resolveSinBandeja(Boolean value) {
            return value == null || value;
        }

    }

    static SigningParam mergeParamDto(SigningParamDto dto) {
        SigningParam defaults = SigningParam.defaults();
        if (dto == null) {
            return defaults;
        }
        return new SigningParam(
                dto.signatureFormat != null ? dto.signatureFormat : defaults.signatureFormat(),
                firstNonBlank(dto.originPath, defaults.originPath()),
                firstNonBlank(dto.destinationPath, defaults.destinationPath()),
                firstNonBlank(dto.fileName, defaults.fileName()),
                dto.signatureStyle != null ? dto.signatureStyle : defaults.signatureStyle(),
                dto.applyImage != null ? dto.applyImage : defaults.applyImage(),
                dto.stampTextSize != null ? dto.stampTextSize : defaults.stampTextSize(),
                dto.stampWordWrap != null ? dto.stampWordWrap : defaults.stampWordWrap(),
                firstNonBlank(dto.signatureTextTemplate, defaults.signatureTextTemplate()),
                firstNonBlank(dto.positioningMode, defaults.positioningMode()),
                firstNonBlank(dto.signatureTagName, dto.tagName, dto.signatureTag, defaults.signatureTagName()),
                dto.stampPage != null ? dto.stampPage : defaults.stampPage(),
                dto.positionx != null ? dto.positionx : defaults.positionX(),
                dto.positiony != null ? dto.positiony : defaults.positionY(),
                firstNonBlank(dto.signatureLevel, SigningAdminSettings.defaultSignatureLevelCode()),
                firstNonBlank(dto.webTsa, defaults.webTsa()),
                firstNonBlank(dto.userTsa, defaults.userTsa()),
                firstNonBlank(dto.passwordTsa, defaults.passwordTsa()),
                firstNonBlank(dto.signatureReason, defaults.signatureReason()));
    }

    private static final class SigningInvocacionDto {
        String idSolicitud;
        String token;
        String refresh_token;
        String callbackUrl;
        String origenDocumento;
        String rutaLocal;
        String documentoUrl;
        String idDocumentoAlfresco;
        String nodeIdAlfresco;
        String contentType;
        Boolean usarAlfresco;
        Boolean sinVisor;
        Boolean sinBandeja;
        Boolean abrirVisorPrimero;
        String modo;
        Boolean FirmaArrastrar;
        Boolean firmaArrastrar;
        Boolean cerrarAlTerminar;
        Boolean interfazMinima;
        Integer documentVersion;
    }

    private static final class SigningParamDto {
        Integer signatureFormat;
        String originPath;
        String destinationPath;
        String fileName;
        Integer signatureStyle;
        Boolean applyImage;
        Integer stampTextSize;
        Integer stampWordWrap;
        @SerializedName("SignatureTextTemplate")
        String signatureTextTemplate;
        String positioningMode;
        String signatureTagName;
        String tagName;
        String signatureTag;
        Integer stampPage;
        Integer positionx;
        Integer positiony;
        String signatureLevel;
        String webTsa;
        String userTsa;
        String passwordTsa;
        String signatureReason;
        Integer documentVersion;
    }

    private static void logSigningParamFromFile(Path parametriaFile, SigningParam signingParam) {
        System.out.println(
                "[ParametriaLoader] "
                        + parametriaFile.toAbsolutePath()
                        + " -> formato="
                        + signingParam.signatureFormat()
                        + " tag="
                        + (signingParam.effectiveTagMarker().isBlank()
                                ? "(vacio)"
                                : signingParam.effectiveTagMarker())
                        + " modoPos="
                        + signingParam.positioningMode());
        if (signingParam.isTagPositioningMode() && !signingParam.usesTagPlacement()) {
            System.err.println(
                    "[ParametriaLoader] AVISO: positioningMode=TA sin signatureTagName; "
                            + "se usaran coordenadas por defecto ("
                            + signingParam.positionX()
                            + ","
                            + signingParam.positionY()
                            + ").");
        }
    }

    private static String resolveValue(Path parametriaFile, String value) {
        return ParametriaVariables.resolve(value, parametriaFile);
    }

    private static Integer resolveDocumentVersion(Integer... values) {
        for (Integer value : values) {
            if (value != null && value > 0) {
                return value;
            }
        }
        String fromEnv = System.getenv("FIRMA_DOCUMENT_VERSION_ALFRESCO");
        if (fromEnv != null && !fromEnv.isBlank()) {
            try {
                int parsed = Integer.parseInt(fromEnv.trim());
                return parsed > 0 ? parsed : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        String fromProperty = System.getProperty("firma.document.version.alfresco");
        if (fromProperty != null && !fromProperty.isBlank()) {
            try {
                int parsed = Integer.parseInt(fromProperty.trim());
                return parsed > 0 ? parsed : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static byte[] decodeBase64(String value, Path parametriaFile) {
        if (value == null || value.isBlank() || ParametriaVariables.isUnresolvedPlaceholder(value)) {
            return null;
        }
        String trimmed = value.trim();
        int comma = trimmed.indexOf(',');
        if (trimmed.startsWith("data:") && comma > 0) {
            trimmed = trimmed.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(trimmed);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("document/stamp no es Base64 valido", e);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
