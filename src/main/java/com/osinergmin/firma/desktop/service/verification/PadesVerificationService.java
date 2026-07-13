package com.osinergmin.firma.desktop.service.verification;

import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;

import java.util.Optional;

/** Verifica documento firmado; delega en {@link DocumentVerificationService}. */
public final class PadesVerificationService {

    private final DocumentVerificationService delegate = new DocumentVerificationService();

    public VerificationResult verify(PdfViewerConfig signedDocument) {
        return delegate.verify(signedDocument, Optional.empty(), SigningParam.defaults());
    }

    public VerificationResult verify(
            PdfViewerConfig signedDocument, Optional<byte[]> originalBytes, SigningParam param) {
        return delegate.verify(signedDocument, originalBytes, param);
    }
}
