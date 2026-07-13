package com.osinergmin.firma.desktop.core.document;

import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;

import java.util.Objects;
import java.util.Optional;

/** Documento preparado y firmado para descarga/visor (independiente de la UI). */
public final class SigningDocumentContext {

    private static volatile PdfViewerConfig preparedDocument;
    private static volatile PdfViewerConfig signedDocument;

    private SigningDocumentContext() {}

    public static void set(PdfViewerConfig config) {
        preparedDocument = config;
        signedDocument = null;
    }

    public static Optional<PdfViewerConfig> getPreparedDocument() {
        return Optional.ofNullable(preparedDocument);
    }

    public static void setSignedDocument(PdfViewerConfig config) {
        signedDocument = config;
    }

    /** Registra el PDF firmado como documento activo (descarga, visor y fallback). */
    public static void commitSignedDocument(PdfViewerConfig signed) {
        Objects.requireNonNull(signed, "signed");
        signedDocument = signed;
        preparedDocument = signed;
    }

    public static Optional<PdfViewerConfig> getSignedDocument() {
        return Optional.ofNullable(signedDocument);
    }

    /** Preferido cuando aún no hay firma; post-firma usar {@link #getSignedDocument()}. */
    public static Optional<PdfViewerConfig> getDocumentForUse() {
        return getSignedDocument().or(SigningDocumentContext::getPreparedDocument);
    }

    public static void clear() {
        preparedDocument = null;
        signedDocument = null;
    }
}
