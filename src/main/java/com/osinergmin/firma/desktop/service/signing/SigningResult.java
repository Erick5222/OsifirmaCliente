package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;

import java.util.Objects;

public final class SigningResult {

    private final boolean success;
    private final String message;
    private final PdfViewerConfig signedDocument;

    private SigningResult(boolean success, String message, PdfViewerConfig signedDocument) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.signedDocument = signedDocument;
    }

    public static SigningResult success(String message, PdfViewerConfig signedDocument) {
        Objects.requireNonNull(signedDocument, "signedDocument");
        return new SigningResult(true, message, signedDocument);
    }

    public static SigningResult failure(String message) {
        return new SigningResult(false, message, null);
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public PdfViewerConfig signedDocument() {
        return signedDocument;
    }
}
