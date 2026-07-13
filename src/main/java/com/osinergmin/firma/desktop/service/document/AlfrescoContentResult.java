package com.osinergmin.firma.desktop.service.document;

public record AlfrescoContentResult(
        boolean success,
        String message,
        byte[] pdfBytes,
        String fileName,
        String contentType) {

    public AlfrescoContentResult {
        pdfBytes = pdfBytes == null ? new byte[0] : pdfBytes.clone();
    }

    public static AlfrescoContentResult ok(byte[] pdfBytes, String fileName, String contentType) {
        return new AlfrescoContentResult(true, "", pdfBytes, fileName, contentType);
    }

    public static AlfrescoContentResult fail(String message) {
        String msg = message == null || message.isBlank() ? "No se pudo obtener el documento." : message.trim();
        return new AlfrescoContentResult(false, msg, new byte[0], "", "");
    }
}
