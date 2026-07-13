package com.osinergmin.firma.desktop.core.certificate;

import com.osinergmin.firma.desktop.service.certificate.CertificateListResult;

import java.util.Optional;

/**
 * Cache en memoria de certificados cargados durante el arranque (overlay de login).
 */
public final class CertificateCatalog {

    private static volatile CertificateListResult cached;

    private CertificateCatalog() {}

    public static void set(CertificateListResult result) {
        cached = result;
    }

    public static Optional<CertificateListResult> getIfLoaded() {
        return Optional.ofNullable(cached);
    }

    public static void clear() {
        cached = null;
    }
}
