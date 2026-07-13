package com.osinergmin.firma.desktop.service.document.dto;

public final class AlfrescoUploadRequestDto {

    private String fileName;
    private String base64Content;
    private String contentType;
    private Integer sourceVersion;

    public AlfrescoUploadRequestDto(
            String fileName, String base64Content, String contentType, Integer sourceVersion) {
        this.fileName = fileName;
        this.base64Content = base64Content;
        this.contentType = contentType;
        this.sourceVersion = sourceVersion;
    }
}
