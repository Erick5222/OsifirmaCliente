package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;

import java.util.Optional;

/** @deprecated Usar {@link DocumentSigningService}; se mantiene por compatibilidad. */
@Deprecated
public final class PadesSigningService {

    private final DocumentSigningService delegate = new DocumentSigningService();

    public String describePlacementForLog(InvokeConfig invokeConfig, Optional<SignaturePlacement> placement) {
        return delegate.describePlacementForLog(invokeConfig, placement);
    }

    public SigningResult sign(
            PdfViewerConfig sourceDocument,
            String certificateAlias,
            InvokeConfig invokeConfig,
            Optional<SignaturePlacement> placement) {
        return delegate.sign(sourceDocument, certificateAlias, invokeConfig, placement);
    }
}
