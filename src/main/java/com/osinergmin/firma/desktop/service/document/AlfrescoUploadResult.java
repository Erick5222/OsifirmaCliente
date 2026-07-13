package com.osinergmin.firma.desktop.service.document;

public record AlfrescoUploadResult(boolean success, String message) {

    public static AlfrescoUploadResult ok(String message) {
        return new AlfrescoUploadResult(true, message == null || message.isBlank() ? "OK" : message.trim());
    }

    public static AlfrescoUploadResult fail(String message) {
        String msg = message == null || message.isBlank() ? "Error al subir a Alfresco." : message.trim();
        return new AlfrescoUploadResult(false, msg);
    }
}
