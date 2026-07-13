package com.osinergmin.firma.desktop.service.certificate;

import java.util.Collections;
import java.util.List;

public record CertificateListResult(
        boolean success, boolean noCertificatesFound, String message, List<InstalledCertificate> certificates) {

    public CertificateListResult {
        certificates = certificates == null ? List.of() : List.copyOf(certificates);
    }

    public static CertificateListResult success(List<InstalledCertificate> certificates) {
        if (certificates == null || certificates.isEmpty()) {
            return noneFound();
        }
        return new CertificateListResult(true, false, "", certificates);
    }

    public static CertificateListResult noneFound() {
        return new CertificateListResult(
                true,
                true,
                "No se encontraron certificados de firma en este equipo.",
                List.of());
    }

    public static CertificateListResult fail(String message) {
        String msg = message == null || message.isBlank() ? "No se pudieron cargar los certificados." : message.trim();
        return new CertificateListResult(false, false, msg, Collections.emptyList());
    }
}
