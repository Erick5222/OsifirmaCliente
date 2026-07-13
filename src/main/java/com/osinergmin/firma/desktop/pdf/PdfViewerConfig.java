package com.osinergmin.firma.desktop.pdf;

import com.osinergmin.firma.desktop.App;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Configuracion del visor PDF: classpath, archivo local o bytes en memoria (Alfresco).
 */
public record PdfViewerConfig(
        String classpathResourceAbsolute,
        String fileSystemPath,
        byte[] inMemoryBytes,
        String displayFileName,
        int initialZoomPercent,
        String footerCaption) {

    private static final String FOOTER = "V2.4.0-STABLE | SISTEMA DE FIRMA DIGITAL OSINERGMIN";

    public PdfViewerConfig {
        boolean fromClasspath = classpathResourceAbsolute != null && !classpathResourceAbsolute.isBlank();
        boolean fromFile = fileSystemPath != null && !fileSystemPath.isBlank();
        boolean fromMemory = inMemoryBytes != null && inMemoryBytes.length > 0;
        if (!fromClasspath && !fromFile && !fromMemory) {
            throw new IllegalArgumentException("Se requiere classpathResourceAbsolute, fileSystemPath o inMemoryBytes");
        }
        if (displayFileName == null || displayFileName.isBlank()) {
            throw new IllegalArgumentException("displayFileName");
        }
        if (initialZoomPercent < 25 || initialZoomPercent > 400) {
            throw new IllegalArgumentException("initialZoomPercent debe estar entre 25 y 400");
        }
        if (fromMemory) {
            inMemoryBytes = Arrays.copyOf(inMemoryBytes, inMemoryBytes.length);
        }
    }

    public boolean usesLocalFile() {
        return fileSystemPath != null && !fileSystemPath.isBlank();
    }

    public boolean usesInMemoryBytes() {
        return inMemoryBytes != null && inMemoryBytes.length > 0;
    }

    /** Demo: documento empaquetado en resources. */
    public static PdfViewerConfig documentoPruebaDemo() {
        return new PdfViewerConfig(
                "/com/osinergmin/firma/desktop/assets/documento_prueba.pdf",
                null,
                null,
                "documento_prueba.pdf",
                100,
                FOOTER);
    }

    public static PdfViewerConfig fromLocalFile(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        return new PdfViewerConfig(
                null,
                absolute.toString(),
                null,
                absolute.getFileName().toString(),
                100,
                FOOTER);
    }

    public static PdfViewerConfig fromInMemoryPdf(byte[] pdfBytes, String displayFileName) {
        return fromInMemory(pdfBytes, displayFileName);
    }

    /** Documento en memoria (PDF, XML, Office, etc.). */
    public static PdfViewerConfig fromInMemory(byte[] bytes, String displayFileName) {
        return new PdfViewerConfig(null, null, bytes, displayFileName, 100, FOOTER);
    }

    public boolean isPdf() {
        String name = displayFileName == null ? "" : displayFileName.trim().toLowerCase();
        return name.endsWith(".pdf");
    }

    /** Lee los bytes del documento (memoria, disco o classpath). */
    public byte[] readDocumentBytes() throws IOException {
        return readPdfBytes();
    }

    /** Lee los bytes del PDF (memoria, disco o classpath). */
    public byte[] readPdfBytes() throws IOException {
        if (usesInMemoryBytes()) {
            return Arrays.copyOf(inMemoryBytes, inMemoryBytes.length);
        }
        if (usesLocalFile()) {
            return Files.readAllBytes(Path.of(fileSystemPath));
        }
        try (InputStream in = App.class.getResourceAsStream(classpathResourceAbsolute)) {
            if (in == null) {
                throw new IOException("No se encontro el recurso: " + classpathResourceAbsolute);
            }
            return in.readAllBytes();
        }
    }

    /** Nombre sugerido al guardar (conserva extension del documento). */
    public String suggestedSaveFileName() {
        String name = displayFileName == null ? "documento.pdf" : displayFileName.trim();
        if (name.isEmpty()) {
            return "documento.pdf";
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            return name;
        }
        return name + ".pdf";
    }
}
