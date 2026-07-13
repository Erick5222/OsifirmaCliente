package com.osinergmin.firma.desktop.config;

/** Configuracion de firma editable desde el panel de administracion del cliente. */
public record SigningAdminConfig(
        String tslUrl,
        boolean verifyTsl,
        String tsaUrl,
        String tsaUser,
        String tsaPassword,
        String signatureLevel,
        String signatureAlgorithmNote) {

    public static SigningAdminConfig defaults() {
        return new SigningAdminConfig(
                ClientConfiguration.getBundledSigningTslUrl(),
                ClientConfiguration.getBundledSigningVerifyTsl(),
                "",
                "",
                "",
                "B",
                "Definido por motor certificado (firmaOsinergmin)");
    }
}
