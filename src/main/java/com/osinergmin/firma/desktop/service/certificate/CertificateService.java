package com.osinergmin.firma.desktop.service.certificate;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import pe.gob.osinergmin.firma.servicios.ServicioCertificado;

/**
 * Lista certificados del almacen personal de Windows usando la misma logica de titular/emisor que firmaOsinergmin.
 */
public final class CertificateService {

    private static final String WINDOWS_PERSONAL_STORE = "Windows-MY";

    private static final DateTimeFormatter EXPIRATION_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-PE"));

    public CertificateListResult listInstalled() {
        ServicioCertificado certificateHelper = new ServicioCertificado();
        try {
            KeyStore keyStore = KeyStore.getInstance(WINDOWS_PERSONAL_STORE);
            keyStore.load(null, null);

            List<InstalledCertificate> installed = new ArrayList<>();
            Date now = new Date();
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (!keyStore.isKeyEntry(alias)) {
                    continue;
                }

                X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
                if (certificate == null || !ServicioCertificado.isNonRepudiation(certificate)) {
                    continue;
                }

                String subjectCn = safeText(certificateHelper.obtenerSubjectCN(certificate));
                String issuerCn = safeText(certificateHelper.obtenerIssuerCN(certificate));
                if (subjectCn.isEmpty()) {
                    subjectCn = safeText(certificate.getSubjectX500Principal().getName());
                }
                if (issuerCn.isEmpty()) {
                    issuerCn = safeText(certificate.getIssuerX500Principal().getName());
                }

                Date notAfter = certificate.getNotAfter();
                boolean vigente = notAfter != null && notAfter.after(now);
                installed.add(
                        new InstalledCertificate(
                                alias,
                                inferTipo(subjectCn),
                                subjectCn,
                                issuerCn,
                                formatExpiration(notAfter),
                                vigente));
            }

            return CertificateListResult.success(installed);
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = ex.getClass().getSimpleName();
            }
            return CertificateListResult.fail(message);
        }
    }

    private static String inferTipo(String subjectCn) {
        String lower = subjectCn.toLowerCase(Locale.ROOT);
        if (lower.contains("dnie") || lower.endsWith(" hard")) {
            return "Smart card";
        }
        if (lower.endsWith(" soft") || lower.endsWith(" sw") || lower.contains(" sw ")) {
            return "Software";
        }
        return "Certificado digital";
    }

    private static String formatExpiration(Date notAfter) {
        if (notAfter == null) {
            return "—";
        }
        return EXPIRATION_FORMAT.format(notAfter.toInstant().atZone(ZoneId.systemDefault()));
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
