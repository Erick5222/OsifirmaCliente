package com.osinergmin.firma.desktop.service.certificate;

/**
 * Certificado de firma detectado en el almacen local (Windows-MY / MSCAPI).
 *
 * @param libraryAlias alias del KeyStore Windows-MY enviado a firmaOsinergmin
 */
public record InstalledCertificate(
        String libraryAlias,
        String tipo,
        String titular,
        String emisor,
        String expiracion,
        boolean vigente) {}
