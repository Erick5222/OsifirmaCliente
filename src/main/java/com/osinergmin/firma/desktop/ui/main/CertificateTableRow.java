package com.osinergmin.firma.desktop.ui.main;



import com.osinergmin.firma.desktop.service.certificate.InstalledCertificate;



/**

 * Fila del selector de certificados en el escritorio institucional.

 */

public final class CertificateTableRow {



    private final InstalledCertificate certificate;



    public CertificateTableRow(InstalledCertificate certificate) {

        this.certificate = certificate;

    }



    public static CertificateTableRow fromInstalled(InstalledCertificate certificate) {

        return new CertificateTableRow(certificate);

    }



    public InstalledCertificate getCertificate() {

        return certificate;

    }



    public String getLibraryAlias() {

        return certificate.libraryAlias();

    }



    public String getTipo() {

        return certificate.tipo();

    }



    public String getTitular() {

        return certificate.titular();

    }



    public String getEmisor() {

        return certificate.emisor();

    }



    public String getExpiracion() {

        return certificate.expiracion();

    }



    public boolean isVerificado() {

        return certificate.vigente();

    }



    public String getEstadoLabel() {

        return isVerificado() ? "VERIFICADO" : "EXPIRADO";

    }



    public boolean isExpired() {

        return !isVerificado();

    }

}

