package com.osinergmin.firma.desktop.service.signing;

/** Posición del sello gráfico en el visor PDF, convertida luego a coordenadas PAdES. */
public record SignaturePlacement(
        int pageNumberOneBased,
        double overlayX,
        double overlayY,
        double overlayWidth,
        double overlayHeight,
        double stampWidth,
        double stampHeight,
        float pageWidthPoints,
        float pageHeightPoints) {}
