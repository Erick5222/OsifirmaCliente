package com.osinergmin.firma.desktop.integration.invoke;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Parametros de invocacion del firmador (JSON, CLI o URL de protocolo).
 * Incluye bloque {@code invocacion}, contrato {@code param} y payloads {@code document}/{@code stamp}.
 */
public final class InvokeConfig {

    private final String idSolicitud;
    private final String token;
    private final String refreshToken;
    private final String callbackUrl;
    private final String origenDocumento;
    private final String rutaLocal;
    private final String documentoUrl;
    private final String idDocumentoAlfresco;
    private final String contentType;
    private final boolean usarAlfresco;
    private final boolean sinVisor;
    private final boolean sinBandeja;
    private final boolean abrirVisorPrimero;
    private final String modo;
    private final boolean firmaArrastrar;
    private final boolean cerrarAlTerminar;
    private final boolean interfazMinima;
    private final String credential;
    private final SigningParam signingParam;
    private final Integer documentVersion;
    private final byte[] documentBytes;
    private final byte[] stampBytes;

    public InvokeConfig(
            String idSolicitud,
            String token,
            String refreshToken,
            String callbackUrl,
            String origenDocumento,
            String rutaLocal,
            String documentoUrl,
            String idDocumentoAlfresco,
            String contentType,
            boolean usarAlfresco,
            boolean sinVisor,
            boolean sinBandeja,
            boolean abrirVisorPrimero,
            String modo,
            boolean firmaArrastrar,
            boolean cerrarAlTerminar,
            boolean interfazMinima,
            String credential,
            SigningParam signingParam,
            Integer documentVersion,
            byte[] documentBytes,
            byte[] stampBytes) {
        this.idSolicitud = nullToEmpty(idSolicitud);
        this.token = nullToEmpty(token);
        this.refreshToken = nullToEmpty(refreshToken);
        this.callbackUrl = nullToEmpty(callbackUrl);
        this.origenDocumento = nullToEmpty(origenDocumento).isEmpty() ? "local" : origenDocumento.trim();
        this.rutaLocal = nullToEmpty(rutaLocal);
        this.documentoUrl = nullToEmpty(documentoUrl);
        this.idDocumentoAlfresco = nullToEmpty(idDocumentoAlfresco);
        this.contentType = nullToEmpty(contentType);
        this.usarAlfresco = usarAlfresco;
        this.sinVisor = sinVisor;
        this.sinBandeja = sinBandeja;
        this.abrirVisorPrimero = abrirVisorPrimero;
        this.modo = nullToEmpty(modo).isEmpty() ? "completo" : modo.trim();
        this.firmaArrastrar = firmaArrastrar;
        this.cerrarAlTerminar = cerrarAlTerminar;
        this.interfazMinima = interfazMinima;
        this.credential = nullToEmpty(credential);
        this.signingParam = signingParam != null ? signingParam : SigningParam.defaults();
        this.documentVersion = documentVersion != null && documentVersion > 0 ? documentVersion : null;
        this.documentBytes = copyBytes(documentBytes);
        this.stampBytes = copyBytes(stampBytes);
    }

    /** Compatibilidad con constructor anterior. */
    public InvokeConfig(
            String idSolicitud,
            String token,
            String refreshToken,
            String origenDocumento,
            String rutaLocal,
            String documentoUrl,
            String idDocumentoAlfresco,
            String contentType,
            boolean usarAlfresco,
            boolean sinVisor,
            boolean sinBandeja,
            boolean abrirVisorPrimero,
            String modo) {
        this(
                idSolicitud,
                token,
                refreshToken,
                "",
                origenDocumento,
                rutaLocal,
                documentoUrl,
                idDocumentoAlfresco,
                contentType,
                usarAlfresco,
                sinVisor,
                sinBandeja,
                abrirVisorPrimero,
                modo,
                true,
                false,
                false,
                "",
                SigningParam.defaults(),
                null,
                null,
                null);
    }

    public static InvokeConfig defaultDemo() {
        return new InvokeConfig("", "", "", "local", "", "", "", "", false, false, false, false, "completo");
    }

    public String getIdSolicitud() {
        return idSolicitud;
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getOrigenDocumento() {
        return origenDocumento;
    }

    public String getRutaLocal() {
        return rutaLocal;
    }

    public String getDocumentoUrl() {
        return documentoUrl;
    }

    public String getIdDocumentoAlfresco() {
        return idDocumentoAlfresco;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isUsarAlfresco() {
        return usarAlfresco;
    }

    public boolean usesAlfrescoDocument() {
        return usarAlfresco && !idDocumentoAlfresco.isBlank();
    }

    public boolean isSinVisor() {
        return sinVisor;
    }

    public boolean shouldSkipDocumentViewer() {
        return sinVisor
                || "soloFirma".equalsIgnoreCase(modo)
                || signingParam.signatureFormat() != 1;
    }

    /** Visor PDF solo aplica a PAdES con flujo completo. */
    public boolean canUsePdfViewer() {
        return signingParam.signatureFormat() == 1 && !shouldSkipDocumentViewer();
    }

    public boolean isSinBandeja() {
        return sinBandeja;
    }

    public boolean isAbrirVisorPrimero() {
        return abrirVisorPrimero;
    }

    public String getModo() {
        return modo;
    }

    /** Si true, el visor permite arrastrar el sello; si false, usa posicion del param. */
    public boolean isFirmaArrastrar() {
        return firmaArrastrar;
    }

    /** Si true, cierra la aplicacion automaticamente tras una firma exitosa. */
    public boolean isCerrarAlTerminar() {
        return cerrarAlTerminar || interfazMinima;
    }

    /** Si true, muestra solo progreso minimo durante la firma (sin stepper ni log en pantalla). */
    public boolean isInterfazMinima() {
        return interfazMinima;
    }

    public String getCredential() {
        return credential;
    }

    public SigningParam signingParam() {
        return signingParam;
    }

    /** Version Alfresco consultada/firmada (ej. 3). Default 1 si origen alfresco y no se indica. */
    public OptionalInt getDocumentVersion() {
        return documentVersion != null ? OptionalInt.of(documentVersion) : OptionalInt.empty();
    }

    public boolean usesAlfrescoVersioning() {
        return documentVersion != null && documentVersion > 0;
    }

    public int targetDocumentVersion() {
        return documentVersion != null ? documentVersion + 1 : 0;
    }

    public Optional<byte[]> documentBytes() {
        return documentBytes == null || documentBytes.length == 0
                ? Optional.empty()
                : Optional.of(Arrays.copyOf(documentBytes, documentBytes.length));
    }

    public Optional<byte[]> stampBytes() {
        return stampBytes == null || stampBytes.length == 0
                ? Optional.empty()
                : Optional.of(Arrays.copyOf(stampBytes, stampBytes.length));
    }

    public boolean hasInlineDocument() {
        return documentBytes != null && documentBytes.length > 0;
    }

    public boolean usesDocumentUrl() {
        return "url".equalsIgnoreCase(origenDocumento) && !documentoUrl.isBlank();
    }

    public boolean hasToken() {
        return !token.isBlank() && !ParametriaVariables.isUnresolvedPlaceholder(token);
    }

    public boolean hasCallbackUrl() {
        return !callbackUrl.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static byte[] copyBytes(byte[] source) {
        if (source == null || source.length == 0) {
            return null;
        }
        return Arrays.copyOf(source, source.length);
    }

    @Override
    public String toString() {
        return "InvokeConfig{origen='" + origenDocumento + "', modo='" + modo + "', sinVisor=" + sinVisor
                + ", interfazMinima=" + interfazMinima + ", firmaArrastrar=" + firmaArrastrar
                + ", usarAlfresco=" + usarAlfresco + ", nodeId='"
                + idDocumentoAlfresco + "', rutaLocal='" + rutaLocal + "', inlineDoc=" + hasInlineDocument()
                + ", callback=" + !callbackUrl.isBlank() + "}";
    }
}
