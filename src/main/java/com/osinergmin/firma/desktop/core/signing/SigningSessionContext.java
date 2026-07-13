package com.osinergmin.firma.desktop.core.signing;

import com.osinergmin.firma.desktop.service.certificate.InstalledCertificate;
import com.osinergmin.firma.desktop.service.signing.PdfTagPositionResolver;
import com.osinergmin.firma.desktop.service.signing.SignaturePlacement;

import java.util.Arrays;
import java.util.Optional;

/** Certificado seleccionado y posicion grafica de firma para la operacion en curso. */
public final class SigningSessionContext {

    private static volatile InstalledCertificate selectedCertificate;
    private static volatile SignaturePlacement signaturePlacement;
    private static volatile byte[] originalDocumentBytes;
    private static volatile PdfTagPositionResolver.TagPosition resolvedTagPosition;

    private SigningSessionContext() {}

    public static void setSelectedCertificate(InstalledCertificate certificate) {
        selectedCertificate = certificate;
    }

    public static Optional<InstalledCertificate> getSelectedCertificate() {
        return Optional.ofNullable(selectedCertificate);
    }

    public static void setSignaturePlacement(SignaturePlacement placement) {
        signaturePlacement = placement;
    }

    public static Optional<SignaturePlacement> getSignaturePlacement() {
        return Optional.ofNullable(signaturePlacement);
    }

    public static void setOriginalDocumentBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            originalDocumentBytes = null;
            return;
        }
        originalDocumentBytes = Arrays.copyOf(bytes, bytes.length);
    }

    public static Optional<byte[]> getOriginalDocumentBytes() {
        return originalDocumentBytes == null || originalDocumentBytes.length == 0
                ? Optional.empty()
                : Optional.of(Arrays.copyOf(originalDocumentBytes, originalDocumentBytes.length));
    }

    public static void clearSignaturePlacement() {
        signaturePlacement = null;
    }

    public static void setResolvedTagPosition(PdfTagPositionResolver.TagPosition position) {
        resolvedTagPosition = position;
    }

    public static Optional<PdfTagPositionResolver.TagPosition> getResolvedTagPosition() {
        return Optional.ofNullable(resolvedTagPosition);
    }

    public static void clearResolvedTagPosition() {
        resolvedTagPosition = null;
    }

    public static void clear() {
        selectedCertificate = null;
        signaturePlacement = null;
        originalDocumentBytes = null;
        resolvedTagPosition = null;
    }
}
