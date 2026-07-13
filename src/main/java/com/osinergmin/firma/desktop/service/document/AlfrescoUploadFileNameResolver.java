package com.osinergmin.firma.desktop.service.document;

import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;
import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;
import com.osinergmin.firma.desktop.service.signing.SigningDocumentNaming;

import java.util.Locale;

/** Nombre del archivo enviado a Alfresco al terminar la firma. */
public final class AlfrescoUploadFileNameResolver {

    private AlfrescoUploadFileNameResolver() {}

    /**
     * Nombre al subir a Alfresco. Prioridad: {@code param.fileName} (literal en parametria) &gt;
     * nombre del documento firmado en sesion &gt; default.
     */
    public static String resolve(InvokeConfig invokeConfig, PdfViewerConfig signedDocument) {
        if (invokeConfig != null) {
            SigningParam param = invokeConfig.signingParam();
            String fromParam = param.fileName();
            if (fromParam != null && !fromParam.isBlank()) {
                return fromParam.trim();
            }
        }
        if (signedDocument != null && signedDocument.displayFileName() != null) {
            String signed = signedDocument.displayFileName().trim();
            if (!signed.isBlank()) {
                return signed;
            }
        }
        if (invokeConfig != null) {
            return SigningDocumentNaming.buildSignedDisplayName("documento", invokeConfig.signingParam());
        }
        return "documento[F].pdf";
    }

    /** Compatibilidad PAdES: quita sufijo [F].pdf para derivar nombre base (tests legacy). */
    static String stripSignedSuffix(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "documento.pdf";
        }
        String trimmed = fileName.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.endsWith("[f].pdf")) {
            return trimmed.substring(0, trimmed.length() - 7) + ".pdf";
        }
        return trimmed;
    }
}
