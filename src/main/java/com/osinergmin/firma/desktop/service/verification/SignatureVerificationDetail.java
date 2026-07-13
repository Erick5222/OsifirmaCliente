package com.osinergmin.firma.desktop.service.verification;

/** Detalle de una firma dentro de {@link VerificationResult}. */
public record SignatureVerificationDetail(
        int index,
        String firmadoPor,
        String estadoFirma,
        String fechaFirma,
        String formatoFirma,
        String motivo,
        String algoritmo,
        String notBefore,
        String notAfter) {}
