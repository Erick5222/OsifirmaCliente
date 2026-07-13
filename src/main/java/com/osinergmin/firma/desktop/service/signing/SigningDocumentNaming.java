package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.integration.invoke.SigningParam;

import java.nio.file.Path;
import java.util.Locale;

/** Extensiones y nombres de archivo segun formato de firma (PAdES / XAdES / CAdES). */
public final class SigningDocumentNaming {

    private SigningDocumentNaming() {}

    public static String inputExtensionFor(SigningParam param, String displayFileName) {
        int format = param != null ? param.signatureFormat() : 1;
        return switch (format) {
            case 2 -> ".xml";
            case 3 -> extensionOf(displayFileName, ".bin");
            default -> extensionOf(displayFileName, ".pdf");
        };
    }

    public static String outputExtensionFor(SigningParam param) {
        int format = param != null ? param.signatureFormat() : 1;
        return switch (format) {
            case 2 -> ".xml";
            case 3 -> ".p7s";
            default -> ".pdf";
        };
    }

    public static String buildSignedDisplayName(String originalName, SigningParam param) {
        String base = originalName == null || originalName.isBlank() ? defaultBaseName(param) : originalName.trim();
        String outExt = outputExtensionFor(param);
        int dot = base.lastIndexOf('.');
        String stem = dot > 0 ? base.substring(0, dot) : base;
        if (stem.endsWith("[F]")) {
            return stem + outExt;
        }
        return stem + "[F]" + outExt;
    }

    public static Path buildSignedSiblingPath(Path input, SigningParam param) {
        String fileName = input.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        String outExt = outputExtensionFor(param);
        return input.resolveSibling(stem + "[F]" + outExt);
    }

    public static boolean isPdfDocument(String displayFileName) {
        return extensionOf(displayFileName, "").equalsIgnoreCase(".pdf");
    }

    public static boolean isXmlDocument(String displayFileName) {
        return extensionOf(displayFileName, "").equalsIgnoreCase(".xml");
    }

    public static OptionalFormatValidation validateForSigning(SigningParam param, String displayFileName) {
        if (param == null) {
            return OptionalFormatValidation.ok();
        }
        return switch (param.signatureFormat()) {
            case 2 -> {
                if (!isXmlDocument(displayFileName)) {
                    yield OptionalFormatValidation.error(
                            "XAdES requiere un documento XML (.xml). Nombre actual: " + displayFileName);
                }
                yield OptionalFormatValidation.ok();
            }
            case 3 -> OptionalFormatValidation.ok();
            default -> {
                if (!isPdfDocument(displayFileName)) {
                    yield OptionalFormatValidation.error(
                            "PAdES requiere un PDF (.pdf). Nombre actual: "
                                    + displayFileName
                                    + ". Para imagenes u otros archivos use signatureFormat=3 (CAdES).");
                }
                yield OptionalFormatValidation.ok();
            }
        };
    }

    public static String guessMimeType(String fileName) {
        String ext = extensionOf(fileName, "").toLowerCase(Locale.ROOT);
        return switch (ext) {
            case ".pdf" -> "application/pdf";
            case ".xml" -> "application/xml";
            case ".p7s" -> "application/pkcs7-signature";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    private static String defaultBaseName(SigningParam param) {
        return switch (param != null ? param.signatureFormat() : 1) {
            case 2 -> "documento.xml";
            case 3 -> "documento.bin";
            default -> "documento.pdf";
        };
    }

    private static String extensionOf(String fileName, String fallback) {
        if (fileName == null || fileName.isBlank()) {
            return fallback;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot >= fileName.length() - 1) {
            return fallback;
        }
        return fileName.substring(dot).toLowerCase(Locale.ROOT);
    }

    public record OptionalFormatValidation(boolean valid, String message) {
        static OptionalFormatValidation ok() {
            return new OptionalFormatValidation(true, "");
        }

        static OptionalFormatValidation error(String message) {
            return new OptionalFormatValidation(false, message);
        }
    }
}
