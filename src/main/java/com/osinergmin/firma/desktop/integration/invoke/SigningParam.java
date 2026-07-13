package com.osinergmin.firma.desktop.integration.invoke;

import com.osinergmin.firma.desktop.config.ClientConfiguration;

/** Parametros de firma del contrato oficial (objeto {@code param}). */
public record SigningParam(
        int signatureFormat,
        String originPath,
        String destinationPath,
        String fileName,
        int signatureStyle,
        boolean applyImage,
        int stampTextSize,
        int stampWordWrap,
        String signatureTextTemplate,
        String positioningMode,
        String signatureTagName,
        int stampPage,
        int positionX,
        int positionY,
        String signatureLevel,
        String webTsa,
        String userTsa,
        String passwordTsa,
        String signatureReason) {

    public static SigningParam defaults() {
        return new SigningParam(
                1,
                "",
                "",
                "documento.pdf",
                1,
                true,
                ClientConfiguration.getSigningTextSize(),
                37,
                "",
                "",
                "",
                1,
                ClientConfiguration.getSigningDefaultPosX(),
                ClientConfiguration.getSigningDefaultPosY(),
                "B",
                "",
                "",
                "",
                ClientConfiguration.getSigningReason());
    }

    public String standardFirma() {
        return switch (signatureFormat) {
            case 2 -> "XAdES";
            case 3 -> "CAdES";
            default -> "PAdES";
        };
    }

    public int signatureLevelCode() {
        if (signatureLevel == null) {
            return 1;
        }
        return switch (signatureLevel.trim().toUpperCase()) {
            case "T" -> 2;
            case "LTA" -> 3;
            default -> 1;
        };
    }

    public boolean hasPositioningMode() {
        return positioningMode != null && !positioningMode.isBlank();
    }

    /** Firma anclada a texto en el PDF cuando {@code signatureTagName} esta definido. */
    public boolean usesTagPlacement() {
        return signatureTagName != null && !signatureTagName.isBlank();
    }

    public boolean isTagPositioningMode() {
        return positioningMode != null && "TA".equalsIgnoreCase(positioningMode.trim());
    }

    public String effectiveTagMarker() {
        if (signatureTagName != null && !signatureTagName.isBlank()) {
            return signatureTagName.trim();
        }
        return "";
    }
}
