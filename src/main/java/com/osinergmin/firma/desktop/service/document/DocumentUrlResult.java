package com.osinergmin.firma.desktop.service.document;

public record DocumentUrlResult(boolean success, String message, byte[] pdfBytes, String fileName) {

    public static DocumentUrlResult success(byte[] pdfBytes, String fileName) {
        return new DocumentUrlResult(true, "OK", pdfBytes, fileName == null ? "documento.pdf" : fileName);
    }

    public static DocumentUrlResult fail(String message) {
        return new DocumentUrlResult(false, message, null, "");
    }
}
